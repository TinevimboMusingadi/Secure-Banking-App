package com.securebank.security;

import com.securebank.model.Role;
import com.securebank.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Manages authenticated user session state, session timeout, and access controls.
 * Adheres to secure session management principles.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class SessionContext {

    private static final Duration SESSION_TIMEOUT = Duration.ofMinutes(15);

    private final String sessionId;
    private final User currentUser;
    private final Instant loginTime;
    private Instant lastActivityTime;

    public SessionContext(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null for an active session");
        }
        this.sessionId = UUID.randomUUID().toString();
        this.currentUser = user;
        this.loginTime = Instant.now();
        this.lastActivityTime = Instant.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public Instant getLastActivityTime() {
        return lastActivityTime;
    }

    public void touch() {
        this.lastActivityTime = Instant.now();
    }

    public boolean isExpired() {
        return Duration.between(lastActivityTime, Instant.now()).compareTo(SESSION_TIMEOUT) > 0;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    public boolean isAuthorizedForUser(String targetUserId) {
        if (currentUser == null) return false;
        if (isAdmin()) return true;
        return currentUser.getUserId().equals(targetUserId);
    }
}
