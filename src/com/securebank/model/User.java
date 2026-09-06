package com.securebank.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Encapsulated domain entity representing a system user with built-in
 * defense against brute-force attacks via progressive account lockout.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class User {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final String userId;
    private final String username;
    private String passwordHash;
    private String salt;
    private final String fullName;
    private Role role;
    private boolean locked;
    private int failedLoginAttempts;
    private Instant lockoutTimestamp;
    private final Instant createdAt;

    public User(String userId, String username, String passwordHash, String salt,
                String fullName, Role role, boolean locked, int failedLoginAttempts,
                Instant lockoutTimestamp, Instant createdAt) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash cannot be null");
        this.salt = Objects.requireNonNull(salt, "salt cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName cannot be null");
        this.role = role != null ? role : Role.CUSTOMER;
        this.locked = locked;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockoutTimestamp = lockoutTimestamp;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static User createNew(String userId, String username, String passwordHash,
                                String salt, String fullName, Role role) {
        return new User(userId, username, passwordHash, salt, fullName, role, false, 0, null, Instant.now());
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isLocked() {
        if (locked && lockoutTimestamp != null) {
            // Check for automatic expiration of temporary lockout
            if (Duration.between(lockoutTimestamp, Instant.now()).compareTo(LOCKOUT_DURATION) >= 0) {
                unlock();
                return false;
            }
        }
        return locked;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockoutTimestamp() {
        return lockoutTimestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Increments failed attempt counter and locks the account if the threshold is reached.
     * Thwarts automated credential-stuffing and brute-force attacks.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            this.locked = true;
            this.lockoutTimestamp = Instant.now();
        }
    }

    /**
     * Resets the failed attempts counter upon successful authentication.
     */
    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.locked = false;
        this.lockoutTimestamp = null;
    }

    /**
     * Unlocks the account (by admin intervention or timer expiration).
     */
    public void unlock() {
        this.locked = false;
        this.failedLoginAttempts = 0;
        this.lockoutTimestamp = null;
    }

    public void updatePassword(String newHash, String newSalt) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash cannot be null");
        this.salt = Objects.requireNonNull(newSalt, "newSalt cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        // Redact sensitive credentials in string representations to prevent log leakage
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", locked=" + locked +
                '}';
    }
}
