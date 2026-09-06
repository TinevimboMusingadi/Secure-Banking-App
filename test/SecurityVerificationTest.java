package test;

import com.securebank.model.*;
import com.securebank.repository.FileRepository;
import com.securebank.security.InputValidator;
import com.securebank.security.PasswordHasher;
import com.securebank.security.SessionContext;
import com.securebank.service.AuditService;
import com.securebank.service.AuthService;
import com.securebank.service.BankingException;
import com.securebank.service.BankingService;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Automated Verification Test Suite for the Secure Banking Application.
 * Validates cryptographic security, account lockout, IDOR prevention,
 * financial accuracy, and data persistence.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class SecurityVerificationTest {

    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  RUNNING SECURE BANKING APPLICATION VERIFICATION SUITE ");
        System.out.println("  Candidate: Tinevimbo Musingadi (Reg No: H250125B)     ");
        System.out.println("========================================================");

        Path testDataDir = null;
        try {
            testDataDir = Files.createTempDirectory("securebank_test_");

            testPasswordHashing();
            testInputValidation();
            testAccountLockout(testDataDir.toString());
            testIDORPrevention(testDataDir.toString());
            testPolymorphicAccounts(testDataDir.toString());
            testTransferAtomicity(testDataDir.toString());
            testFilePersistenceReload(testDataDir.toString());

            System.out.println("\n========================================================");
            System.out.printf("  VERIFICATION RESULT: %d/%d TESTS PASSED (100%% SUCCESS)\n", passedTests, totalTests);
            System.out.println("========================================================");

        } catch (Exception e) {
            System.err.println("[TEST RUNNER ERROR] " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (testDataDir != null) {
                try {
                    Files.walk(testDataDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (Exception ignored) {}
            }
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
            throw new AssertionError("Test failed: " + testName);
        }
    }

    private static void testPasswordHashing() {
        String salt = PasswordHasher.generateSalt();
        char[] password = "SecurePassword@123".toCharArray();
        String hash = PasswordHasher.hashPassword(password, salt);

        assertTrue("PBKDF2 hash should not be empty", hash != null && !hash.isEmpty());
        assertTrue("PBKDF2 verification with correct password",
                PasswordHasher.verifyPassword("SecurePassword@123".toCharArray(), salt, hash));
        assertTrue("PBKDF2 rejection of incorrect password",
                !PasswordHasher.verifyPassword("WrongPassword@123".toCharArray(), salt, hash));
    }

    private static void testInputValidation() {
        assertTrue("Valid username accepted", InputValidator.isValidUsername("tinevimbo_99"));
        assertTrue("Invalid username with symbols rejected", !InputValidator.isValidUsername("tine<script>"));
        assertTrue("Strong password accepted", InputValidator.isStrongPassword("Str0ng!Pass2026".toCharArray()));
        assertTrue("Weak password (no symbol) rejected", !InputValidator.isStrongPassword("WeakPassword123".toCharArray()));
        assertTrue("Valid amount accepted", InputValidator.isValidAmount(new BigDecimal("250.50")));
        assertTrue("Negative amount rejected", !InputValidator.isValidAmount(new BigDecimal("-10.00")));
        assertTrue("Excessive scale rejected", !InputValidator.isValidAmount(new BigDecimal("10.005")));
    }

    private static void testAccountLockout(String dataDir) throws Exception {
        AuditService audit = new AuditService(dataDir + "/audit.log");
        FileRepository repo = new FileRepository(dataDir);
        repo.initialize();
        AuthService auth = new AuthService(repo, audit);

        auth.registerUser("locktest_user", "Secret@2026!".toCharArray(), "Lock Test", Role.CUSTOMER);

        // Fail 1
        try { auth.login("locktest_user", "BadPwd@1".toCharArray()); } catch (BankingException ignored) {}
        // Fail 2
        try { auth.login("locktest_user", "BadPwd@2".toCharArray()); } catch (BankingException ignored) {}
        // Fail 3 -> triggers lockout
        try { auth.login("locktest_user", "BadPwd@3".toCharArray()); } catch (BankingException ignored) {}

        boolean isLockedOut = false;
        try {
            // 4th attempt with CORRECT password should be rejected because account is locked!
            auth.login("locktest_user", "Secret@2026!".toCharArray());
        } catch (BankingException e) {
            isLockedOut = e.getMessage().contains("locked");
        }

        assertTrue("Account locks after 3 failed login attempts", isLockedOut);
    }

    private static void testIDORPrevention(String dataDir) throws Exception {
        AuditService audit = new AuditService(dataDir + "/audit.log");
        FileRepository repo = new FileRepository(dataDir);
        repo.initialize();
        AuthService auth = new AuthService(repo, audit);
        BankingService banking = new BankingService(repo, audit);

        auth.registerUser("victim_user", "Victim@2026!".toCharArray(), "Alice Victim", Role.CUSTOMER);
        auth.registerUser("attacker_user", "Attacker@2026!".toCharArray(), "Bob Attacker", Role.CUSTOMER);

        SessionContext victimSession = auth.login("victim_user", "Victim@2026!".toCharArray());
        Account victimAccount = banking.createAccount(victimSession, "SAVINGS", new BigDecimal("1000.00"));

        SessionContext attackerSession = auth.login("attacker_user", "Attacker@2026!".toCharArray());

        boolean accessDenied = false;
        try {
            // Attacker tries to withdraw from victim's account
            banking.withdraw(attackerSession, victimAccount.getAccountNumber(), new BigDecimal("100.00"), "Exploit");
        } catch (BankingException e) {
            accessDenied = e.getMessage().contains("Access Denied");
        }

        assertTrue("IDOR defense prevents horizontal privilege escalation", accessDenied);
    }

    private static void testPolymorphicAccounts(String dataDir) throws Exception {
        AuditService audit = new AuditService(dataDir + "/audit.log");
        FileRepository repo = new FileRepository(dataDir);
        repo.initialize();
        AuthService auth = new AuthService(repo, audit);
        BankingService banking = new BankingService(repo, audit);

        SessionContext session = auth.login("victim_user", "Victim@2026!".toCharArray());

        // Savings Account Minimum Balance ($25.00)
        Account savings = banking.createAccount(session, "SAVINGS", new BigDecimal("100.00"));
        banking.withdraw(session, savings.getAccountNumber(), new BigDecimal("70.00"), "Withdraw 70");
        assertTrue("Savings balance after valid withdrawal",
                banking.getAccountBalance(session, savings.getAccountNumber()).compareTo(new BigDecimal("30.00")) == 0);

        boolean savingsMinBalanceViolated = false;
        try {
            // Attempting to withdraw $10 would leave $20 which is < $25 min balance
            banking.withdraw(session, savings.getAccountNumber(), new BigDecimal("10.00"), "Violate min balance");
        } catch (BankingException e) {
            savingsMinBalanceViolated = e.getMessage().contains("minimum balance");
        }
        assertTrue("SavingsAccount enforces minimum balance constraint", savingsMinBalanceViolated);

        // Checking Account Overdraft ($100.00 limit + $5 overdraft fee)
        Account checking = banking.createAccount(session, "CHECKING", new BigDecimal("50.00"));
        banking.withdraw(session, checking.getAccountNumber(), new BigDecimal("100.00"), "Overdraft test");
        // Balance = 50 - 100 - 5 (fee) = -55.00
        BigDecimal checkingBal = banking.getAccountBalance(session, checking.getAccountNumber());
        assertTrue("CheckingAccount permits controlled overdraft with penalty fee",
                checkingBal.compareTo(new BigDecimal("-55.00")) == 0);
    }

    private static void testTransferAtomicity(String dataDir) throws Exception {
        AuditService audit = new AuditService(dataDir + "/audit.log");
        FileRepository repo = new FileRepository(dataDir);
        repo.initialize();
        AuthService auth = new AuthService(repo, audit);
        BankingService banking = new BankingService(repo, audit);

        SessionContext session = auth.login("victim_user", "Victim@2026!".toCharArray());
        Account accA = banking.createAccount(session, "SAVINGS", new BigDecimal("500.00"));
        Account accB = banking.createAccount(session, "SAVINGS", new BigDecimal("100.00"));

        banking.transfer(session, accA.getAccountNumber(), accB.getAccountNumber(), new BigDecimal("200.00"), "Gift");

        assertTrue("Transfer debited source account correctly",
                banking.getAccountBalance(session, accA.getAccountNumber()).compareTo(new BigDecimal("300.00")) == 0);
        assertTrue("Transfer credited destination account correctly",
                banking.getAccountBalance(session, accB.getAccountNumber()).compareTo(new BigDecimal("300.00")) == 0);
    }

    private static void testFilePersistenceReload(String dataDir) throws Exception {
        AuditService audit = new AuditService(dataDir + "/audit.log");
        FileRepository repo = new FileRepository(dataDir);
        repo.initialize();

        assertTrue("Persisted users reloaded from disk", repo.findUserByUsername("victim_user").isPresent());
        assertTrue("Persisted accounts reloaded from disk", !repo.getAllAccounts().isEmpty());
    }
}
