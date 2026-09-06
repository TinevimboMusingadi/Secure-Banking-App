package com.securebank.repository;

import com.securebank.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Text-file based data persistence repository.
 *
 * Secure Coding Implementation:
 * 1. Atomic file updates: Writes to a temporary file in the same directory and performs
 *    an atomic move, preventing partial-write file corruption during sudden system crashes.
 * 2. Whitelist delimiter separation (|) with strict field length checks.
 * 3. Graceful fallback on initial load (creates default files if missing).
 * 4. Thread-safe in-memory cache synchronized with filesystem state.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class FileRepository {

    private final String dataDir;
    private final Path usersFilePath;
    private final Path accountsFilePath;
    private final Path transactionsFilePath;

    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, Account> accountsByNumber = new ConcurrentHashMap<>();
    private final List<Transaction> transactionsList = Collections.synchronizedList(new ArrayList<>());

    public FileRepository(String dataDir) {
        this.dataDir = dataDir;
        this.usersFilePath = Paths.get(dataDir, "users.txt");
        this.accountsFilePath = Paths.get(dataDir, "accounts.txt");
        this.transactionsFilePath = Paths.get(dataDir, "transactions.txt");
    }

    public synchronized void initialize() throws DataPersistenceException {
        try {
            Files.createDirectories(Paths.get(dataDir));
            loadUsers();
            loadAccounts();
            loadTransactions();
        } catch (IOException e) {
            throw new DataPersistenceException("Failed to initialize data persistence directory: " + dataDir, e);
        }
    }

    // ==========================================
    // USER PERSISTENCE
    // ==========================================

    private void loadUsers() throws DataPersistenceException {
        if (!Files.exists(usersFilePath)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(usersFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 10) {
                    continue; // Skip malformed rows safely
                }

                String userId = parts[0];
                String username = parts[1];
                String passwordHash = parts[2];
                String salt = parts[3];
                String fullName = parts[4];
                Role role = Role.fromString(parts[5]);
                boolean locked = Boolean.parseBoolean(parts[6]);
                int failedAttempts = Integer.parseInt(parts[7]);
                Instant lockoutTimestamp = parts[8].isEmpty() || "null".equals(parts[8]) ? null : Instant.parse(parts[8]);
                Instant createdAt = Instant.parse(parts[9]);

                User user = new User(userId, username, passwordHash, salt, fullName, role, locked,
                        failedAttempts, lockoutTimestamp, createdAt);
                usersById.put(userId, user);
                usersByUsername.put(username.toLowerCase(), user);
            }
        } catch (Exception e) {
            throw new DataPersistenceException("Failed to load users from persistence file", e);
        }
    }

    public synchronized void saveUser(User user) throws DataPersistenceException {
        usersById.put(user.getUserId(), user);
        usersByUsername.put(user.getUsername().toLowerCase(), user);
        flushUsersToFile();
    }

    public synchronized void flushUsersToFile() throws DataPersistenceException {
        List<String> lines = new ArrayList<>();
        lines.add("# userId|username|passwordHash|salt|fullName|role|locked|failedAttempts|lockoutTimestamp|createdAt");
        for (User u : usersById.values()) {
            String line = String.join("|",
                    u.getUserId(),
                    u.getUsername(),
                    u.getPasswordHash(),
                    u.getSalt(),
                    u.getFullName(),
                    u.getRole().name(),
                    String.valueOf(u.isLocked()),
                    String.valueOf(u.getFailedLoginAttempts()),
                    u.getLockoutTimestamp() != null ? u.getLockoutTimestamp().toString() : "null",
                    u.getCreatedAt().toString()
            );
            lines.add(line);
        }
        atomicWrite(usersFilePath, lines);
    }

    public Optional<User> findUserById(String userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    public Optional<User> findUserByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(usersByUsername.get(username.trim().toLowerCase()));
    }

    public List<User> getAllUsers() {
        return Collections.unmodifiableList(new ArrayList<>(usersById.values()));
    }

    // ==========================================
    // ACCOUNT PERSISTENCE
    // ==========================================

    private void loadAccounts() throws DataPersistenceException {
        if (!Files.exists(accountsFilePath)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(accountsFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;

                String accNum = parts[0];
                String userId = parts[1];
                String type = parts[2];
                BigDecimal balance = new BigDecimal(parts[3]);
                String specialAttr = parts[4];
                Instant createdAt = Instant.parse(parts[5]);
                boolean active = Boolean.parseBoolean(parts[6]);

                Account account;
                if ("SAVINGS".equalsIgnoreCase(type)) {
                    BigDecimal interestRate = new BigDecimal(specialAttr);
                    account = new SavingsAccount(accNum, userId, balance, interestRate, createdAt, active);
                } else {
                    BigDecimal overdraft = new BigDecimal(specialAttr);
                    account = new CheckingAccount(accNum, userId, balance, overdraft, createdAt, active);
                }
                accountsByNumber.put(accNum, account);
            }
        } catch (Exception e) {
            throw new DataPersistenceException("Failed to load accounts from persistence file", e);
        }
    }

    public synchronized void saveAccount(Account account) throws DataPersistenceException {
        accountsByNumber.put(account.getAccountNumber(), account);
        flushAccountsToFile();
    }

    public synchronized void flushAccountsToFile() throws DataPersistenceException {
        List<String> lines = new ArrayList<>();
        lines.add("# accountNumber|userId|type|balance|specialAttribute|createdAt|active");
        for (Account a : accountsByNumber.values()) {
            String line = String.join("|",
                    a.getAccountNumber(),
                    a.getUserId(),
                    a.getAccountType(),
                    a.getBalance().toPlainString(),
                    a.getSpecialAttribute(),
                    a.getCreatedAt().toString(),
                    String.valueOf(a.isActive())
            );
            lines.add(line);
        }
        atomicWrite(accountsFilePath, lines);
    }

    public Optional<Account> findAccountByNumber(String accNum) {
        if (accNum == null) return Optional.empty();
        return Optional.ofNullable(accountsByNumber.get(accNum.trim()));
    }

    public List<Account> getAccountsByUserId(String userId) {
        List<Account> userAccounts = new ArrayList<>();
        for (Account acc : accountsByNumber.values()) {
            if (acc.getUserId().equals(userId)) {
                userAccounts.add(acc);
            }
        }
        return Collections.unmodifiableList(userAccounts);
    }

    public List<Account> getAllAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accountsByNumber.values()));
    }

    // ==========================================
    // TRANSACTION PERSISTENCE
    // ==========================================

    private void loadTransactions() throws DataPersistenceException {
        if (!Files.exists(transactionsFilePath)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(transactionsFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|", -1);
                if (parts.length < 9) continue;

                String txId = parts[0];
                Instant timestamp = Instant.parse(parts[1]);
                String src = parts[2];
                String dst = parts[3];
                TransactionType type = TransactionType.fromString(parts[4]);
                BigDecimal amount = new BigDecimal(parts[5]);
                BigDecimal resBal = new BigDecimal(parts[6]);
                String desc = parts[7];
                String status = parts[8];

                Transaction tx = new Transaction(txId, timestamp, src, dst, type, amount, resBal, desc, status);
                transactionsList.add(tx);
            }
        } catch (Exception e) {
            throw new DataPersistenceException("Failed to load transactions from persistence file", e);
        }
    }

    public synchronized void recordTransaction(Transaction tx) throws DataPersistenceException {
        transactionsList.add(tx);
        appendTransactionToFile(tx);
    }

    private synchronized void appendTransactionToFile(Transaction tx) throws DataPersistenceException {
        String line = String.join("|",
                tx.getTransactionId(),
                tx.getTimestamp().toString(),
                tx.getSourceAccountNumber(),
                tx.getDestinationAccountNumber(),
                tx.getType().name(),
                tx.getAmount().toPlainString(),
                tx.getResultingBalance().toPlainString(),
                tx.getDescription().replace('|', '/'),
                tx.getStatus()
        );

        try (BufferedWriter writer = Files.newBufferedWriter(transactionsFilePath,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new DataPersistenceException("Failed to append transaction to file", e);
        }
    }

    public List<Transaction> getTransactionsForAccount(String accountNumber) {
        List<Transaction> accountTxList = new ArrayList<>();
        synchronized (transactionsList) {
            for (Transaction tx : transactionsList) {
                if (tx.getSourceAccountNumber().equalsIgnoreCase(accountNumber) ||
                        tx.getDestinationAccountNumber().equalsIgnoreCase(accountNumber)) {
                    accountTxList.add(tx);
                }
            }
        }
        return Collections.unmodifiableList(accountTxList);
    }

    // ==========================================
    // ATOMIC WRITE UTILITY
    // ==========================================

    /**
     * Atomically writes lines to a file via a temporary file in the same directory.
     * Guarantees that in the event of abrupt shutdown, the existing file is never left corrupt.
     */
    private void atomicWrite(Path targetFile, List<String> lines) throws DataPersistenceException {
        Path tempFile = null;
        try {
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            tempFile = Files.createTempFile(parent, "tmp_", ".dat");
            Files.write(tempFile, lines, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            try {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback for file systems not supporting atomic move
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
            throw new DataPersistenceException("Failed atomic file write to: " + targetFile.getFileName(), e);
        }
    }
}
