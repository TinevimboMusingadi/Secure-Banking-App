package com.securebank.service;

import com.securebank.model.*;
import com.securebank.repository.DataPersistenceException;
import com.securebank.repository.FileRepository;
import com.securebank.security.InputValidator;
import com.securebank.security.SessionContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core Banking Operations Service.
 *
 * Secure Coding Implementations:
 * 1. Broken Object Level Authorization (BOLA/IDOR) Mitigation:
 *    Ensures customers can ONLY view, deposit into, and withdraw from accounts they own.
 * 2. Race Condition Prevention:
 *    Consistent locking hierarchy across accounts during transfers to avoid deadlocks.
 * 3. Exact Financial Arithmetic:
 *    All calculations conducted via BigDecimal with RoundingMode.HALF_UP.
 * 4. Audit & Non-Repudiation:
 *    Every financial activity is recorded to both transaction persistence and audit trail.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class BankingService {

    private final FileRepository fileRepository;
    private final AuditService auditService;
    private final AtomicInteger accountSequence = new AtomicInteger(100000);

    public BankingService(FileRepository fileRepository, AuditService auditService) {
        this.fileRepository = Objects.requireNonNull(fileRepository, "fileRepository required");
        this.auditService = Objects.requireNonNull(auditService, "auditService required");
        initializeSequence();
    }

    private void initializeSequence() {
        int maxSeq = 100000;
        for (Account acc : fileRepository.getAllAccounts()) {
            try {
                String numPart = acc.getAccountNumber().replace("ACC-", "");
                int seq = Integer.parseInt(numPart);
                if (seq > maxSeq) {
                    maxSeq = seq;
                }
            } catch (Exception ignored) {}
        }
        accountSequence.set(maxSeq);
    }

    private String generateAccountNumber() {
        return "ACC-" + accountSequence.incrementAndGet();
    }

    // ==========================================
    // ACCOUNT CREATION & INQUIRY
    // ==========================================

    /**
     * Creates a new bank account (Savings or Checking) for the authenticated user.
     */
    public Account createAccount(SessionContext session, String accountType, BigDecimal initialDeposit) throws BankingException {
        session.touch();
        User user = session.getCurrentUser();

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BankingException("Initial deposit cannot be negative.");
        }

        BigDecimal startingBalance = initialDeposit != null ? initialDeposit : BigDecimal.ZERO;
        String accNum = generateAccountNumber();
        Account newAccount;

        if ("SAVINGS".equalsIgnoreCase(accountType)) {
            // Default 3.5% APY
            newAccount = new SavingsAccount(accNum, user.getUserId(), startingBalance,
                    new BigDecimal("0.0350"), Instant.now(), true);
        } else if ("CHECKING".equalsIgnoreCase(accountType)) {
            // Default $100.00 Overdraft Protection
            newAccount = new CheckingAccount(accNum, user.getUserId(), startingBalance,
                    new BigDecimal("100.00"), Instant.now(), true);
        } else {
            throw new BankingException("Unsupported account type: " + accountType + ". Must be SAVINGS or CHECKING.");
        }

        try {
            fileRepository.saveAccount(newAccount);

            if (startingBalance.compareTo(BigDecimal.ZERO) > 0) {
                Transaction initialTx = Transaction.createSuccessful(accNum, "INITIAL",
                        TransactionType.DEPOSIT, startingBalance, startingBalance, "Initial opening deposit");
                fileRepository.recordTransaction(initialTx);
            }

            auditService.logEvent(user.getUsername(), "ACCOUNT_CREATION", "SUCCESS",
                    "Created " + accountType.toUpperCase() + " account: " + accNum + " with balance: $" + startingBalance);
            return newAccount;
        } catch (DataPersistenceException e) {
            auditService.logEvent(user.getUsername(), "ACCOUNT_CREATION", "FAILURE", e.getMessage());
            throw new BankingException("Failed to persist new bank account", e);
        }
    }

    /**
     * Retrieves all accounts belonging to the authenticated user.
     */
    public List<Account> getMyAccounts(SessionContext session) {
        session.touch();
        return fileRepository.getAccountsByUserId(session.getCurrentUser().getUserId());
    }

    /**
     * Checks account balance with authorization verification (IDOR protection).
     */
    public BigDecimal getAccountBalance(SessionContext session, String accountNumber) throws BankingException {
        session.touch();
        Account account = getAuthorizedAccount(session, accountNumber);
        return account.getBalance();
    }

    // ==========================================
    // TRANSACTIONS: DEPOSIT & WITHDRAW
    // ==========================================

    /**
     * Deposits funds into an account with positive amount validation and IDOR check.
     */
    public Transaction deposit(SessionContext session, String accountNumber, BigDecimal amount, String description) throws BankingException {
        session.touch();
        if (!InputValidator.isValidAmount(amount)) {
            throw new BankingException("Invalid deposit amount. Must be positive, up to 2 decimal places, and <= $1,000,000.00.");
        }

        Account account = getAuthorizedAccount(session, accountNumber);

        synchronized (account) {
            account.deposit(amount);
            BigDecimal newBalance = account.getBalance();

            Transaction tx = Transaction.createSuccessful(accountNumber, "CASH_DEPOSIT",
                    TransactionType.DEPOSIT, amount, newBalance,
                    description != null ? description : "Cash Deposit");

            try {
                fileRepository.saveAccount(account);
                fileRepository.recordTransaction(tx);
                auditService.logEvent(session.getCurrentUser().getUsername(), "DEPOSIT", "SUCCESS",
                        String.format("Account: %s, Amount: $%s, NewBalance: $%s", accountNumber, amount, newBalance));
                return tx;
            } catch (DataPersistenceException e) {
                // Rollback in-memory state on persistence failure
                account.withdraw(amount);
                auditService.logEvent(session.getCurrentUser().getUsername(), "DEPOSIT", "ROLLBACK", e.getMessage());
                throw new BankingException("Transaction could not be completed due to a storage failure", e);
            }
        }
    }

    /**
     * Withdraws funds from an account with polymorphic balance verification and IDOR check.
     */
    public Transaction withdraw(SessionContext session, String accountNumber, BigDecimal amount, String description) throws BankingException {
        session.touch();
        if (!InputValidator.isValidAmount(amount)) {
            throw new BankingException("Invalid withdrawal amount. Must be positive, up to 2 decimal places, and <= $1,000,000.00.");
        }

        Account account = getAuthorizedAccount(session, accountNumber);

        synchronized (account) {
            BigDecimal previousBalance = account.getBalance();
            try {
                account.withdraw(amount);
            } catch (BankingException ex) {
                // Record failed transaction attempt
                Transaction failedTx = Transaction.createFailed(accountNumber, "CASH_WITHDRAWAL",
                        TransactionType.WITHDRAWAL, amount, previousBalance, ex.getMessage());
                try {
                    fileRepository.recordTransaction(failedTx);
                } catch (DataPersistenceException ignored) {}
                auditService.logEvent(session.getCurrentUser().getUsername(), "WITHDRAWAL", "FAILURE",
                        "Account: " + accountNumber + ", Reason: " + ex.getMessage());
                throw ex;
            }

            BigDecimal newBalance = account.getBalance();
            Transaction tx = Transaction.createSuccessful(accountNumber, "CASH_WITHDRAWAL",
                    TransactionType.WITHDRAWAL, amount, newBalance,
                    description != null ? description : "Cash Withdrawal");

            try {
                fileRepository.saveAccount(account);
                fileRepository.recordTransaction(tx);
                auditService.logEvent(session.getCurrentUser().getUsername(), "WITHDRAWAL", "SUCCESS",
                        String.format("Account: %s, Amount: $%s, NewBalance: $%s", accountNumber, amount, newBalance));
                return tx;
            } catch (DataPersistenceException e) {
                // Rollback withdrawal on persistence failure
                account.deposit(amount);
                auditService.logEvent(session.getCurrentUser().getUsername(), "WITHDRAWAL", "ROLLBACK", e.getMessage());
                throw new BankingException("Transaction could not be completed due to storage failure", e);
            }
        }
    }

    /**
     * Transfers funds between two accounts.
     * Prevents deadlocks by enforcing deterministic lock acquisition order.
     */
    public void transfer(SessionContext session, String sourceAccNum, String destAccNum,
                         BigDecimal amount, String description) throws BankingException {
        session.touch();
        if (sourceAccNum.equalsIgnoreCase(destAccNum)) {
            throw new BankingException("Source and destination accounts cannot be identical.");
        }
        if (!InputValidator.isValidAmount(amount)) {
            throw new BankingException("Invalid transfer amount. Must be positive and <= $1,000,000.00.");
        }

        Account source = getAuthorizedAccount(session, sourceAccNum);
        Optional<Account> destOpt = fileRepository.findAccountByNumber(destAccNum);
        if (!destOpt.isPresent()) {
            throw new BankingException("Destination account not found: " + destAccNum);
        }
        Account dest = destOpt.get();

        // Enforce deterministic locking order based on account numbers to prevent deadlocks
        Account firstLock = source.getAccountNumber().compareTo(dest.getAccountNumber()) < 0 ? source : dest;
        Account secondLock = firstLock == source ? dest : source;

        synchronized (firstLock) {
            synchronized (secondLock) {
                source.withdraw(amount);
                dest.deposit(amount);

                Transaction debitTx = Transaction.createSuccessful(sourceAccNum, destAccNum,
                        TransactionType.TRANSFER_OUT, amount, source.getBalance(),
                        "Transfer to " + destAccNum + (description != null ? " - " + description : ""));
                Transaction creditTx = Transaction.createSuccessful(destAccNum, sourceAccNum,
                        TransactionType.TRANSFER_IN, amount, dest.getBalance(),
                        "Transfer from " + sourceAccNum + (description != null ? " - " + description : ""));

                try {
                    fileRepository.saveAccount(source);
                    fileRepository.saveAccount(dest);
                    fileRepository.recordTransaction(debitTx);
                    fileRepository.recordTransaction(creditTx);

                    auditService.logEvent(session.getCurrentUser().getUsername(), "TRANSFER", "SUCCESS",
                            String.format("Transferred $%s from %s to %s", amount, sourceAccNum, destAccNum));
                } catch (DataPersistenceException e) {
                    // Rollback both accounts
                    source.deposit(amount);
                    try {
                        dest.withdraw(amount);
                    } catch (Exception ignored) {}
                    throw new BankingException("Transfer failed during persistence and was rolled back.", e);
                }
            }
        }
    }

    /**
     * Retrieves transaction history for an account with authorization check.
     */
    public List<Transaction> getTransactionHistory(SessionContext session, String accountNumber) throws BankingException {
        session.touch();
        getAuthorizedAccount(session, accountNumber); // verifies ownership
        return fileRepository.getTransactionsForAccount(accountNumber);
    }

    /**
     * Authorizes access to an account preventing Insecure Direct Object Reference (IDOR).
     */
    private Account getAuthorizedAccount(SessionContext session, String accountNumber) throws BankingException {
        if (!InputValidator.isValidAccountNumber(accountNumber)) {
            throw new BankingException("Invalid account number format: " + accountNumber);
        }

        Optional<Account> accOpt = fileRepository.findAccountByNumber(accountNumber);
        if (!accOpt.isPresent()) {
            throw new BankingException("Account not found: " + accountNumber);
        }

        Account account = accOpt.get();
        if (!session.isAuthorizedForUser(account.getUserId())) {
            auditService.logEvent(session.getCurrentUser().getUsername(), "UNAUTHORIZED_ACCOUNT_ACCESS",
                    "SECURITY_ALERT", "Attempted unauthorized access to account: " + accountNumber);
            throw new BankingException("Access Denied: You do not have permission to view or manage this account.");
        }

        return account;
    }
}
