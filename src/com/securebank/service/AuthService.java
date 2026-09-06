package com.securebank.service;

import com.securebank.model.Role;
import com.securebank.model.User;
import com.securebank.repository.DataPersistenceException;
import com.securebank.repository.FileRepository;
import com.securebank.security.InputValidator;
import com.securebank.security.PasswordHasher;
import com.securebank.security.SessionContext;

import java.util.*;

/**
 * Authentication and User Identity Service.
 *
 * Implemented Security Defenses:
 * 1. Brute-Force & Credential Stuffing Defense: Locks account after 3 failed attempts.
 * 2. User Enumeration Mitigation: Uniform error messages ("Invalid credentials") for unknown users and wrong passwords.
 * 3. Timing Attack Mitigation: Executes constant-time dummy verification when user is unknown.
 * 4. Password Policy Enforcement: Minimum 8 chars, mixed case, numbers, special characters.
 * 5. Memory Sanitization: Wiping password char arrays immediately after processing.
 * 6. Non-Repudiation: Every login success, failure, lockout, and registration is audit-logged.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class AuthService {

    private final FileRepository fileRepository;
    private final AuditService auditService;
    // Pre-computed dummy salt/hash for timing mitigation on invalid usernames
    private static final String DUMMY_SALT = "00112233445566778899aabbccddeeff";
    private static final String DUMMY_HASH = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    public AuthService(FileRepository fileRepository, AuditService auditService) {
        this.fileRepository = Objects.requireNonNull(fileRepository, "fileRepository required");
        this.auditService = Objects.requireNonNull(auditService, "auditService required");
    }

    /**
     * Registers a new user account with strict security validation.
     */
    public User registerUser(String username, char[] password, String fullName, Role role) throws BankingException {
        if (!InputValidator.isValidUsername(username)) {
            throw new BankingException("Invalid username. Must be 4-20 alphanumeric characters or underscores.");
        }
        if (!InputValidator.isValidFullName(fullName)) {
            throw new BankingException("Invalid full name. Must contain only letters, spaces, hyphens, and apostrophes.");
        }
        if (!InputValidator.isStrongPassword(password)) {
            throw new BankingException("Password does not meet complexity requirements.\n" +
                    "Must be 8-64 characters and include at least one uppercase letter, one lowercase letter, one digit, and one special character.");
        }
        if (InputValidator.containsDelimiterInjection(username) || InputValidator.containsDelimiterInjection(fullName)) {
            throw new BankingException("Input contains invalid characters.");
        }

        if (fileRepository.findUserByUsername(username).isPresent()) {
            auditService.logEvent(username, "USER_REGISTRATION", "FAILURE", "Username already exists");
            throw new BankingException("Username is already taken. Please choose another.");
        }

        String salt = PasswordHasher.generateSalt();
        String passwordHash = PasswordHasher.hashPassword(password, salt);
        String userId = "USR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User newUser = User.createNew(userId, username, passwordHash, salt, fullName, role);

        try {
            fileRepository.saveUser(newUser);
            auditService.logEvent(username, "USER_REGISTRATION", "SUCCESS", "Role: " + role.name() + ", UserId: " + userId);
            return newUser;
        } catch (DataPersistenceException e) {
            auditService.logEvent(username, "USER_REGISTRATION", "FAILURE", "Persistence failure: " + e.getMessage());
            throw new BankingException("System error: Unable to save new user registration.", e);
        }
    }

    /**
     * Authenticates user credentials with brute-force defense and timing mitigation.
     */
    public SessionContext login(String username, char[] password) throws BankingException {
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            throw new BankingException("Username and password must not be empty.");
        }

        Optional<User> userOpt = fileRepository.findUserByUsername(username);

        if (!userOpt.isPresent()) {
            // Mitigate timing attack by executing dummy verification
            PasswordHasher.verifyPassword(password, DUMMY_SALT, DUMMY_HASH);
            auditService.logEvent(username, "LOGIN_ATTEMPT", "FAILURE", "Unknown username");
            throw new BankingException("Invalid credentials.");
        }

        User user = userOpt.get();

        if (user.isLocked()) {
            auditService.logEvent(username, "LOGIN_ATTEMPT", "BLOCKED", "Account locked due to excessive failed attempts");
            throw new BankingException("Account is temporarily locked due to 3 consecutive failed login attempts.\n" +
                    "Please wait 15 minutes or contact a bank administrator.");
        }

        boolean valid = PasswordHasher.verifyPassword(password, user.getSalt(), user.getPasswordHash());

        if (valid) {
            user.resetFailedLogins();
            try {
                fileRepository.saveUser(user);
            } catch (DataPersistenceException ignored) {}

            auditService.logEvent(username, "LOGIN_ATTEMPT", "SUCCESS", "Role: " + user.getRole().name());
            return new SessionContext(user);
        } else {
            user.recordFailedLogin();
            try {
                fileRepository.saveUser(user);
            } catch (DataPersistenceException ignored) {}

            int remaining = Math.max(0, User.MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts());
            if (user.isLocked()) {
                auditService.logEvent(username, "ACCOUNT_LOCKOUT", "WARNING", "Threshold reached. Account locked for 15 minutes.");
                throw new BankingException("Invalid credentials. Maximum attempts exceeded. Your account has been locked.");
            } else {
                auditService.logEvent(username, "LOGIN_ATTEMPT", "FAILURE", "Wrong password. Remaining attempts: " + remaining);
                throw new BankingException("Invalid credentials. Remaining attempts before lockout: " + remaining);
            }
        }
    }

    /**
     * Changes an authenticated user's password with full re-hashing.
     */
    public void changePassword(SessionContext session, char[] currentPassword, char[] newPassword) throws BankingException {
        User user = session.getCurrentUser();
        boolean valid = PasswordHasher.verifyPassword(currentPassword, user.getSalt(), user.getPasswordHash());
        if (!valid) {
            auditService.logEvent(user.getUsername(), "PASSWORD_CHANGE", "FAILURE", "Current password verification failed");
            throw new BankingException("Current password verification failed.");
        }

        if (!InputValidator.isStrongPassword(newPassword)) {
            throw new BankingException("New password does not meet complexity requirements.");
        }

        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hashPassword(newPassword, newSalt);
        user.updatePassword(newHash, newSalt);

        try {
            fileRepository.saveUser(user);
            auditService.logEvent(user.getUsername(), "PASSWORD_CHANGE", "SUCCESS", "Password updated successfully");
        } catch (DataPersistenceException e) {
            throw new BankingException("Failed to persist updated password", e);
        }
    }

    /**
     * Administrator operation: unlocks a locked user account.
     */
    public void unlockUserAccount(SessionContext adminSession, String targetUsername) throws BankingException {
        if (!adminSession.isAdmin()) {
            auditService.logEvent(adminSession.getCurrentUser().getUsername(), "ADMIN_UNLOCK", "UNAUTHORIZED", "Attempted unauthorized unlock on: " + targetUsername);
            throw new BankingException("Access Denied: Only administrators can unlock accounts.");
        }

        Optional<User> userOpt = fileRepository.findUserByUsername(targetUsername);
        if (!userOpt.isPresent()) {
            throw new BankingException("User not found: " + targetUsername);
        }

        User targetUser = userOpt.get();
        targetUser.unlock();

        try {
            fileRepository.saveUser(targetUser);
            auditService.logEvent(adminSession.getCurrentUser().getUsername(), "ADMIN_UNLOCK", "SUCCESS", "Unlocked account for: " + targetUsername);
        } catch (DataPersistenceException e) {
            throw new BankingException("Failed to save unlocked account state", e);
        }
    }

    public List<User> listAllUsers(SessionContext adminSession) throws BankingException {
        if (!adminSession.isAdmin()) {
            throw new BankingException("Access Denied: Only administrators can view all users.");
        }
        return fileRepository.getAllUsers();
    }
}
