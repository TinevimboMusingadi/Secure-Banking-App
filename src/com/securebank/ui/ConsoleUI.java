package com.securebank.ui;

import com.securebank.model.*;
import com.securebank.security.InputValidator;
import com.securebank.security.SessionContext;
import com.securebank.service.AuditService;
import com.securebank.service.AuthService;
import com.securebank.service.BankingException;
import com.securebank.service.BankingService;

import java.io.Console;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive Console User Interface with ASCII aesthetics, structured menus,
 * and comprehensive error containment.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class ConsoleUI {

    private final AuthService authService;
    private final BankingService bankingService;
    private final AuditService auditService;
    private final Scanner scanner;
    private SessionContext currentSession;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public ConsoleUI(AuthService authService, BankingService bankingService, AuditService auditService) {
        this.authService = authService;
        this.bankingService = bankingService;
        this.auditService = auditService;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("\n  Starting Secure Banking Core...");
        AsciiArt.showProgress("Initializing Cryptographic Enclave & Records", 450);
        System.out.println();
        System.out.println(AsciiArt.MAIN_BANNER);
        System.out.println(AsciiArt.VAULT_ICON);

        boolean running = true;
        while (running) {
            try {
                if (currentSession == null) {
                    running = showGuestMenu();
                } else if (currentSession.isAdmin()) {
                    running = showAdminMenu();
                } else {
                    running = showCustomerMenu();
                }
            } catch (Exception e) {
                System.out.println("\n[!] An unexpected error occurred: " + e.getMessage());
                auditService.logEvent(
                        currentSession != null ? currentSession.getCurrentUser().getUsername() : "ANONYMOUS",
                        "UI_EXCEPTION", "ERROR", e.getMessage()
                );
            }
        }

        System.out.println(AsciiArt.GOODBYE);
    }

    // ==========================================
    // GUEST (UNAUTHENTICATED) MENU
    // ==========================================

    private boolean showGuestMenu() {
        System.out.println("\n+-------------------------------------------------------+");
        System.out.println("|               PORTAL AUTHENTICATION GATEWAY           |");
        System.out.println("+-------------------------------------------------------+");
        System.out.println("|  [1] Secure Login                                     |");
        System.out.println("|  [2] Register New Customer Account                    |");
        System.out.println("|  [3] About & Security Architecture                    |");
        System.out.println("|  [4] Exit Application                                 |");
        System.out.println("+-------------------------------------------------------+");
        System.out.print("  Select option: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                handleLogin();
                break;
            case "2":
                handleRegistration();
                break;
            case "3":
                showSecurityArchitecture();
                break;
            case "4":
                return false;
            default:
                System.out.println("  [!] Invalid selection. Please choose an option from 1 to 4.");
        }
        return true;
    }

    private void handleLogin() {
        System.out.println("\n" + AsciiArt.SHIELD_SECURITY);
        System.out.println("  --- SECURE AUTHENTICATION ---");
        System.out.print("  Enter Username: ");
        String username = scanner.nextLine().trim();

        char[] password = readPasswordWithVisibility("Enter Password for [" + username + "]:", false);

        AsciiArt.showProgress("Deriving PBKDF2 Key (65,536 rounds) & Verifying", 450);

        try {
            this.currentSession = authService.login(username, password);
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            System.out.printf("  Welcome, %s (%s)!\n",
                    currentSession.getCurrentUser().getFullName(),
                    currentSession.getCurrentUser().getRole().getDisplayName());
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void handleRegistration() {
        System.out.println("\n  --- CUSTOMER REGISTRATION ---");
        System.out.println("  [ Banking With Musingadi | Secure Onboarding ]");
        System.out.print("  Enter Desired Username (4-20 alphanumeric chars): ");
        String username = scanner.nextLine().trim();

        System.out.print("  Enter Full Legal Name: ");
        String fullName = scanner.nextLine().trim();

        System.out.println("\n  Password Policy: >= 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 symbol.");
        char[] password = readPasswordWithVisibility("Create Your Account Password:", true);
        char[] confirm = readPasswordWithVisibility("Confirm Your Account Password:", true);

        if (!Arrays.equals(password, confirm)) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Passwords do not match. Registration cancelled.");
            System.out.printf("      First password length   : %d characters\n", password.length);
            System.out.printf("      Confirm password length : %d characters\n", confirm.length);
            System.out.print("      Would you like to reveal both to spot the typo? (y/N): ");
            String inspect = scanner.nextLine().trim();
            if (inspect.equalsIgnoreCase("y") || inspect.equalsIgnoreCase("yes")) {
                System.out.println("      ----------------------------------------------");
                System.out.println("      Password #1 : \"" + new String(password) + "\"");
                System.out.println("      Password #2 : \"" + new String(confirm) + "\"");
                System.out.println("      ----------------------------------------------");
            }
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
            return;
        }

        AsciiArt.showProgress("Generating 128-bit Salt & Initializing Vault Ledger", 550);

        try {
            User newUser = authService.registerUser(username, password, fullName, Role.CUSTOMER);
            // Auto-create a default primary savings account with 0 balance
            SessionContext tempSession = new SessionContext(newUser);
            Account defaultAccount = bankingService.createAccount(tempSession, "SAVINGS", BigDecimal.ZERO);

            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            System.out.println("  Account registered successfully with Musingadi Secure Bank!");
            System.out.println("  Assigned User ID        : " + newUser.getUserId());
            System.out.println("  Primary Savings Account : " + defaultAccount.getAccountNumber());
            System.out.println("  You may now log in.");
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Registration failed: " + e.getMessage());
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
    }

    private void showSecurityArchitecture() {
        System.out.println("\n========================================================");
        System.out.println("         SECURITY & CRYPTOGRAPHIC ARCHITECTURE          ");
        System.out.println("========================================================");
        System.out.println("  Student: Tinevimbo Musingadi (Reg No: H250125B)");
        System.out.println("  Class: Information Security & Assurance - Secure Coding");
        System.out.println("--------------------------------------------------------");
        System.out.println("  [+] Authentication: PBKDF2WithHmacSHA256 (65,536 rounds)");
        System.out.println("  [+] Salt: 128-bit Cryptographically Secure Random Salt");
        System.out.println("  [+] Brute-Force Defense: Account Lockout after 3 tries");
        System.out.println("  [+] Anti-Timing Attacks: Constant-time digest verification");
        System.out.println("  [+] Financial Integrity: BigDecimal Scale 2 Currency Math");
        System.out.println("  [+] Authorization: Zero-Trust IDOR/BOLA Object Checks");
        System.out.println("  [+] Persistence: Atomic File Swaps (Zero corruption)");
        System.out.println("  [+] Auditability: Tamper-Evident Append-Only Log Trail");
        System.out.println("========================================================");
    }

    // ==========================================
    // CUSTOMER MENU
    // ==========================================

    private boolean showCustomerMenu() {
        checkSessionTimeout();
        if (currentSession == null) return true;

        System.out.println("\n+-------------------------------------------------------+");
        System.out.printf("|   CUSTOMER DASHBOARD - %-30s |\n", currentSession.getCurrentUser().getFullName());
        System.out.println("+-------------------------------------------------------+");
        System.out.println("|  [1] View My Accounts & Balances                      |");
        System.out.println("|  [2] Create New Account (Savings / Checking)          |");
        System.out.println("|  [3] Deposit Funds                                    |");
        System.out.println("|  [4] Withdraw Funds                                   |");
        System.out.println("|  [5] Transfer Funds to Another Account                |");
        System.out.println("|  [6] View Transaction Statement                       |");
        System.out.println("|  [7] Change Account Password                          |");
        System.out.println("|  [8] Secure Logout                                    |");
        System.out.println("+-------------------------------------------------------+");
        System.out.print("  Select action: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                viewAccounts();
                break;
            case "2":
                createAccountFlow();
                break;
            case "3":
                depositFlow();
                break;
            case "4":
                withdrawFlow();
                break;
            case "5":
                transferFlow();
                break;
            case "6":
                statementFlow();
                break;
            case "7":
                changePasswordFlow();
                break;
            case "8":
                logout();
                break;
            default:
                System.out.println("  [!] Invalid choice. Please enter 1-8.");
        }
        return true;
    }

    private void viewAccounts() {
        List<Account> accounts = bankingService.getMyAccounts(currentSession);
        System.out.println("\n+--------------------------------------------------------------------------------+");
        System.out.println("|                             MY BANK ACCOUNTS                                   |");
        System.out.println("+---------------+-----------+---------------+--------------------+---------------+");
        System.out.println("| Account No.   | Type      | Balance       | Parameter          | Status        |");
        System.out.println("+---------------+-----------+---------------+--------------------+---------------+");

        if (accounts.isEmpty()) {
            System.out.println("| You do not have any registered accounts yet.                                  |");
        } else {
            for (Account acc : accounts) {
                String param = acc instanceof SavingsAccount ?
                        "Rate: " + ((SavingsAccount) acc).getInterestRate().multiply(new BigDecimal("100")).toPlainString() + "%" :
                        "Overdraft: $" + ((CheckingAccount) acc).getOverdraftLimit().toPlainString();

                System.out.printf("| %-13s | %-9s | $%-12s | %-18s | %-13s |\n",
                        acc.getAccountNumber(),
                        acc.getAccountType(),
                        acc.getBalance().toPlainString(),
                        param,
                        acc.isActive() ? "ACTIVE" : "INACTIVE");
            }
        }
        System.out.println("+---------------+-----------+---------------+--------------------+---------------+");
    }

    private void createAccountFlow() {
        System.out.println("\n  --- OPEN A NEW BANK ACCOUNT ---");
        System.out.println("  Available Account Types:");
        System.out.println("  [1] Savings Account (3.50% APY, $25.00 Minimum Balance)");
        System.out.println("  [2] Checking Account ($100.00 Overdraft Protection)");
        System.out.print("  Select type (1 or 2): ");
        String typeChoice = scanner.nextLine().trim();

        String type;
        if ("1".equals(typeChoice)) {
            type = "SAVINGS";
        } else if ("2".equals(typeChoice)) {
            type = "CHECKING";
        } else {
            System.out.println("  [!] Invalid selection.");
            return;
        }

        System.out.print("  Initial Deposit Amount ($): ");
        String amtStr = scanner.nextLine().trim();
        BigDecimal initialDeposit;
        try {
            initialDeposit = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid monetary amount entered.");
            return;
        }

        try {
            AsciiArt.showProgress("Allocating Account Ledger & Generating Keys", 350);
            Account acc = bankingService.createAccount(currentSession, type, initialDeposit);
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            System.out.println("  Account successfully created!");
            System.out.println("  Account Number : " + acc.getAccountNumber());
            System.out.println("  Account Type   : " + acc.getAccountType());
            System.out.println("  Balance        : $" + acc.getBalance().toPlainString());
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Account creation failed: " + e.getMessage());
        }
    }

    private void depositFlow() {
        System.out.println("\n  --- DEPOSIT FUNDS ---");
        System.out.print("  Enter Destination Account Number (e.g. ACC-100001): ");
        String accNum = scanner.nextLine().trim();

        System.out.print("  Enter Deposit Amount ($): ");
        String amtStr = scanner.nextLine().trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid numeric format.");
            return;
        }

        System.out.print("  Deposit Description / Memo: ");
        String memo = scanner.nextLine().trim();

        AsciiArt.showSpinner("Validating funds and updating persistent ledger...", 8);

        try {
            Transaction tx = bankingService.deposit(currentSession, accNum, amount, memo);
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            AsciiArt.printReceipt(tx.getTransactionId(),
                    DATE_TIME_FORMATTER.format(tx.getTimestamp()),
                    "DEPOSIT", "CASH/ATM", accNum,
                    tx.getAmount().toPlainString(), tx.getResultingBalance().toPlainString());
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Deposit failed: " + e.getMessage());
        }
    }

    private void withdrawFlow() {
        System.out.println("\n  --- WITHDRAW FUNDS ---");
        System.out.print("  Enter Source Account Number (e.g. ACC-100001): ");
        String accNum = scanner.nextLine().trim();

        System.out.print("  Enter Withdrawal Amount ($): ");
        String amtStr = scanner.nextLine().trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid numeric format.");
            return;
        }

        System.out.print("  Withdrawal Note / Memo: ");
        String memo = scanner.nextLine().trim();

        AsciiArt.showSpinner("Checking polymorphic balance limits and dispensing...", 8);

        try {
            Transaction tx = bankingService.withdraw(currentSession, accNum, amount, memo);
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            AsciiArt.printReceipt(tx.getTransactionId(),
                    DATE_TIME_FORMATTER.format(tx.getTimestamp()),
                    "WITHDRAWAL", accNum, "CASH/DISPENSER",
                    tx.getAmount().toPlainString(), tx.getResultingBalance().toPlainString());
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Withdrawal failed: " + e.getMessage());
        }
    }

    private void transferFlow() {
        System.out.println("\n  --- TRANSFER FUNDS ---");
        System.out.print("  Enter Your Source Account Number: ");
        String srcAcc = scanner.nextLine().trim();

        System.out.print("  Enter Recipient Account Number: ");
        String dstAcc = scanner.nextLine().trim();

        System.out.print("  Enter Transfer Amount ($): ");
        String amtStr = scanner.nextLine().trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(amtStr);
        } catch (NumberFormatException e) {
            System.out.println("  [!] Invalid monetary value.");
            return;
        }

        System.out.print("  Reference / Memo: ");
        String memo = scanner.nextLine().trim();

        AsciiArt.showProgress("Acquiring locks & executing atomic transfer", 400);

        try {
            bankingService.transfer(currentSession, srcAcc, dstAcc, amount, memo);
            BigDecimal newBal = bankingService.getAccountBalance(currentSession, srcAcc);
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            System.out.println("  Transfer completed successfully!");
            System.out.printf("  Debited from %s: $%s (Remaining: $%s)\n", srcAcc, amount, newBal);
            System.out.printf("  Credited to %s\n", dstAcc);
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Transfer rejected: " + e.getMessage());
        }
    }

    private void statementFlow() {
        System.out.println("\n  --- ACCOUNT TRANSACTION STATEMENT ---");
        System.out.print("  Enter Account Number: ");
        String accNum = scanner.nextLine().trim();

        try {
            List<Transaction> txList = bankingService.getTransactionHistory(currentSession, accNum);
            System.out.println("\n+---------------------------------------------------------------------------------------------------------+");
            System.out.printf("| STATEMENT FOR ACCOUNT: %-80s |\n", accNum);
            System.out.println("+---------------------+---------------------+---------------+--------------+--------------+---------------+");
            System.out.println("| Timestamp           | Transaction ID      | Operation     | Amount       | Balance      | Status        |");
            System.out.println("+---------------------+---------------------+---------------+--------------+--------------+---------------+");

            if (txList.isEmpty()) {
                System.out.println("| No recorded transactions found for this account.                                                        |");
            } else {
                for (Transaction tx : txList) {
                    String timeStr = DATE_TIME_FORMATTER.format(tx.getTimestamp());
                    String shortId = tx.getTransactionId().substring(0, 8) + "...";
                    String amtFormatted = (tx.getType().isCredit() ? "+" : "-") + "$" + tx.getAmount().toPlainString();
                    System.out.printf("| %-19s | %-19s | %-13s | %-12s | $%-11s | %-13s |\n",
                            timeStr, shortId, tx.getType().getLabel(), amtFormatted,
                            tx.getResultingBalance().toPlainString(), tx.getStatus());
                }
            }
            System.out.println("+---------------------+---------------------+---------------+--------------+--------------+---------------+");
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Statement generation failed: " + e.getMessage());
        }
    }

    private void changePasswordFlow() {
        System.out.println("\n  --- CHANGE PASSWORD ---");
        char[] currentPwd = readPasswordWithVisibility("Enter Current Password:", false);
        char[] newPwd = readPasswordWithVisibility("Enter New Password:", true);
        char[] confirmPwd = readPasswordWithVisibility("Confirm New Password:", true);

        if (!Arrays.equals(newPwd, confirmPwd)) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Passwords do not match.");
            Arrays.fill(currentPwd, '\0');
            Arrays.fill(newPwd, '\0');
            Arrays.fill(confirmPwd, '\0');
            return;
        }

        AsciiArt.showProgress("Re-hashing with new salt and updating record", 400);

        try {
            authService.changePassword(currentSession, currentPwd, newPwd);
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            System.out.println("  Password successfully updated.");
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Password change failed: " + e.getMessage());
        } finally {
            Arrays.fill(currentPwd, '\0');
            Arrays.fill(newPwd, '\0');
            Arrays.fill(confirmPwd, '\0');
        }
    }

    // ==========================================
    // ADMIN (SECURITY OPERATIONS) MENU
    // ==========================================

    private boolean showAdminMenu() {
        checkSessionTimeout();
        if (currentSession == null) return true;

        System.out.println("\n+-------------------------------------------------------+");
        System.out.println("|        ADMINISTRATOR SECURITY OPERATIONS CENTER       |");
        System.out.println("+-------------------------------------------------------+");
        System.out.println("|  [1] View All Registered Users                        |");
        System.out.println("|  [2] Unlock Locked User Account                       |");
        System.out.println("|  [3] Inspect Tamper-Evident Security Audit Logs       |");
        System.out.println("|  [4] View Total Bank Assets & Accounts Overview       |");
        System.out.println("|  [5] Secure Logout                                    |");
        System.out.println("+-------------------------------------------------------+");
        System.out.print("  Select admin action: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                adminListUsers();
                break;
            case "2":
                adminUnlockUser();
                break;
            case "3":
                adminInspectAuditLogs();
                break;
            case "4":
                adminSystemOverview();
                break;
            case "5":
                logout();
                break;
            default:
                System.out.println("  [!] Invalid admin selection.");
        }
        return true;
    }

    private void adminListUsers() {
        try {
            List<User> users = authService.listAllUsers(currentSession);
            System.out.println("\n+-----------------------------------------------------------------------------------------------------+");
            System.out.println("|                                   REGISTERED BANK USERS                                             |");
            System.out.println("+---------------+--------------------+----------------------+------------+------------+---------------+");
            System.out.println("| User ID       | Username           | Full Name            | Role       | Status     | Failed Logins |");
            System.out.println("+---------------+--------------------+----------------------+------------+------------+---------------+");
            for (User u : users) {
                System.out.printf("| %-13s | %-18s | %-20s | %-10s | %-10s | %-13d |\n",
                        u.getUserId(), u.getUsername(), u.getFullName(),
                        u.getRole().name(), u.isLocked() ? "LOCKED" : "ACTIVE",
                        u.getFailedLoginAttempts());
            }
            System.out.println("+---------------+--------------------+----------------------+------------+------------+---------------+");
        } catch (BankingException e) {
            System.out.println("  [!] Error: " + e.getMessage());
        }
    }

    private void adminUnlockUser() {
        System.out.println("\n  --- ADMIN UNLOCK ACCOUNT ---");
        System.out.print("  Enter target username to unlock: ");
        String targetUser = scanner.nextLine().trim();

        try {
            authService.unlockUserAccount(currentSession, targetUser);
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            System.out.println("  Account for user '" + targetUser + "' has been unlocked.");
        } catch (BankingException e) {
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Unlock failed: " + e.getMessage());
        }
    }

    private void adminInspectAuditLogs() {
        System.out.println("\n  --- RECENT AUDIT LOGS (TAMPER-EVIDENT TRAIL) ---");
        List<String> logs = auditService.getRecentLogs(25);
        if (logs.isEmpty()) {
            System.out.println("  No audit logs recorded yet.");
        } else {
            for (String log : logs) {
                System.out.println("  " + log);
            }
        }
    }

    private void adminSystemOverview() {
        List<Account> accounts = bankingService.getMyAccounts(currentSession); // or all accounts
        System.out.println("\n+-------------------------------------------------------+");
        System.out.println("|               BANK CAPITAL & SYSTEM OVERVIEW          |");
        System.out.println("+-------------------------------------------------------+");
        System.out.println("  Platform Status    : SECURE / ONLINE");
        System.out.println("  Cryptography       : PBKDF2WithHmacSHA256 (65k iterations)");
        System.out.println("  Session Id         : " + currentSession.getSessionId());
        System.out.println("  Active Principal   : " + currentSession.getCurrentUser().getUsername());
        System.out.println("+-------------------------------------------------------+");
    }

    // ==========================================
    // SESSION LIFECYCLE & HELPERS
    // ==========================================

    private void checkSessionTimeout() {
        if (currentSession != null && currentSession.isExpired()) {
            auditService.logEvent(currentSession.getCurrentUser().getUsername(),
                    "SESSION_TIMEOUT", "EXPIRED", "Session expired due to inactivity");
            System.out.println("\n" + AsciiArt.WARNING_STAMP);
            System.out.println("  [!] Your session has expired due to 15 minutes of inactivity.");
            System.out.println("  Please log in again.");
            this.currentSession = null;
        }
    }

    private void logout() {
        if (currentSession != null) {
            auditService.logEvent(currentSession.getCurrentUser().getUsername(),
                    "LOGOUT", "SUCCESS", "User logged out voluntarily");
            System.out.println("\n" + AsciiArt.SUCCESS_STAMP);
            System.out.println("  You have safely logged out.");
            currentSession = null;
        }
    }

    private char[] readPasswordPrompt(String prompt) {
        return readPasswordWithVisibility(prompt, false);
    }

    private char[] readPasswordWithVisibility(String prompt, boolean allowVerifyPreview) {
        while (true) {
            System.out.println("\n  " + prompt);
            System.out.println("  Select Input Visibility Mode:");
            System.out.println("    [1] Hidden Mode  (Characters hidden for privacy)");
            System.out.println("    [2] Visible Mode (Characters visible as typed - prevents typos)");
            System.out.print("  Select Mode [1/2, default 1]: ");
            String mode = scanner.nextLine().trim();

            char[] password;
            if ("2".equals(mode)) {
                System.out.print("  Type Password (VISIBLE): ");
                password = scanner.nextLine().toCharArray();
            } else {
                Console console = System.console();
                if (console != null) {
                    password = console.readPassword("  Type Password (HIDDEN): ");
                } else {
                    System.out.print("  Type Password (HIDDEN): ");
                    password = scanner.nextLine().toCharArray();
                }
            }

            if (password == null || password.length == 0) {
                System.out.println("  [!] Password cannot be empty. Please try again.");
                continue;
            }

            if (allowVerifyPreview) {
                System.out.print("  Would you like to reveal/verify the password you just typed? (y/N): ");
                String reveal = scanner.nextLine().trim();
                if (reveal.equalsIgnoreCase("y") || reveal.equalsIgnoreCase("yes")) {
                    System.out.println("  --------------------------------------------------");
                    System.out.println("  [i] ENTERED PASSWORD : \"" + new String(password) + "\"");
                    System.out.println("  [i] Total Length     : " + password.length + " characters");
                    System.out.println("  --------------------------------------------------");
                    System.out.print("  Is this correct? [1] Yes, continue  [2] No, re-type: ");
                    String confirmChoice = scanner.nextLine().trim();
                    if ("2".equals(confirmChoice)) {
                        Arrays.fill(password, '\0');
                        continue;
                    }
                }
            }
            return password;
        }
    }
}
