package com.securebank;

import com.securebank.model.*;
import com.securebank.repository.FileRepository;
import com.securebank.security.PasswordHasher;
import com.securebank.service.AuditService;
import com.securebank.service.AuthService;
import com.securebank.service.BankingService;
import com.securebank.ui.ConsoleUI;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * Main Application Entry Point for the Secure Banking Console Application.
 * Bootstraps persistence, security services, default seed identities, and UI.
 *
 * Student Details:
 * Name: Tinevimbo Musingadi
 * Registration Number: H250125B
 * Class: Information Security & Assurance (ISA) - Secure Coding
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class Main {

    private static final String DATA_DIRECTORY = "data";
    private static final String AUDIT_LOG_PATH = "data/audit.log";

    public static void main(String[] args) {
        try {
            // 1. Initialize Audit Service
            AuditService auditService = new AuditService(AUDIT_LOG_PATH);
            auditService.logEvent("SYSTEM", "APPLICATION_STARTUP", "SUCCESS", "Secure Banking Console Initialized");

            // 2. Register JVM Graceful Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                auditService.logEvent("SYSTEM", "APPLICATION_SHUTDOWN", "SUCCESS", "Graceful system shutdown completed");
            }));

            // 3. Initialize File-Based Data Persistence
            FileRepository fileRepository = new FileRepository(DATA_DIRECTORY);
            fileRepository.initialize();

            // 4. Seed Default Secure Users and Accounts if empty
            seedInitialDataIfEmpty(fileRepository, auditService);

            // 5. Initialize Core Domain Services
            AuthService authService = new AuthService(fileRepository, auditService);
            BankingService bankingService = new BankingService(fileRepository, auditService);

            // 6. Launch Console User Interface
            ConsoleUI consoleUI = new ConsoleUI(authService, bankingService, auditService);
            consoleUI.start();

        } catch (Exception e) {
            System.err.println("\n[FATAL SYSTEM ERROR] Application failed to initialize securely.");
            System.err.println("Cause: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Seeds initial default accounts with salted PBKDF2 hashes if the database files are empty.
     */
    private static void seedInitialDataIfEmpty(FileRepository repository, AuditService auditService) {
        try {
            if (repository.getAllUsers().isEmpty()) {
                // Seed 1: Administrator Account
                String adminSalt = PasswordHasher.generateSalt();
                String adminHash = PasswordHasher.hashPassword("Admin@2026!".toCharArray(), adminSalt);
                User admin = new User("USR-ADMIN01", "admin", adminHash, adminSalt,
                        "System Security Administrator", Role.ADMIN, false, 0, null, Instant.now());
                repository.saveUser(admin);

                // Seed 2: Default Student Account (Tinevimbo Musingadi - H250125B)
                String studentSalt = PasswordHasher.generateSalt();
                String studentHash = PasswordHasher.hashPassword("Secure@2026!".toCharArray(), studentSalt);
                User student = new User("USR-H250125B", "tinevimbo", studentHash, studentSalt,
                        "Tinevimbo Musingadi", Role.CUSTOMER, false, 0, null, Instant.now());
                repository.saveUser(student);

                // Seed Accounts for Student
                Account savings = new SavingsAccount("ACC-100001", student.getUserId(),
                        new BigDecimal("1500.00"), new BigDecimal("0.0350"), Instant.now(), true);
                Account checking = new CheckingAccount("ACC-100002", student.getUserId(),
                        new BigDecimal("500.00"), new BigDecimal("100.00"), Instant.now(), true);

                repository.saveAccount(savings);
                repository.saveAccount(checking);

                // Initial Seed Transactions
                Transaction t1 = Transaction.createSuccessful("ACC-100001", "OPENING",
                        TransactionType.DEPOSIT, new BigDecimal("1500.00"), new BigDecimal("1500.00"),
                        "Initial Opening Balance - Savings");
                Transaction t2 = Transaction.createSuccessful("ACC-100002", "OPENING",
                        TransactionType.DEPOSIT, new BigDecimal("500.00"), new BigDecimal("500.00"),
                        "Initial Opening Balance - Checking");

                repository.recordTransaction(t1);
                repository.recordTransaction(t2);

                auditService.logEvent("SYSTEM", "DATA_SEEDING", "SUCCESS",
                        "Default administrator (admin) and student account (tinevimbo / H250125B) initialized.");
            }
        } catch (Exception e) {
            auditService.logEvent("SYSTEM", "DATA_SEEDING", "FAILURE", e.getMessage());
        }
    }
}
