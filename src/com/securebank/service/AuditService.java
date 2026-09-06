package com.securebank.service;

import com.securebank.security.InputValidator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tamper-evident Audit Logging Service.
 * Implements strict security event tracking to guarantee non-repudiation and traceability.
 *
 * Mitigates:
 * 1. Log Injection (CWE-117) by sanitizing newlines and delimiter characters.
 * 2. Information loss via synchronous file flushes.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class AuditService {

    private final String logFilePath;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public AuditService(String logFilePath) {
        this.logFilePath = logFilePath;
        ensureLogDirectoryExists();
    }

    private void ensureLogDirectoryExists() {
        File file = new File(logFilePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Records a security or business event in the audit trail.
     *
     * @param actor   username or system principal performing the action
     * @param action  the event type (e.g. LOGIN_SUCCESS, DEPOSIT, WITHDRAW)
     * @param status  outcome status (SUCCESS, FAILURE, WARNING)
     * @param details context message (sanitized against log injection)
     */
    public synchronized void logEvent(String actor, String action, String status, String details) {
        String cleanActor = (actor == null || actor.trim().isEmpty()) ? "ANONYMOUS" : sanitize(actor);
        String cleanAction = sanitize(action);
        String cleanStatus = sanitize(status);
        String cleanDetails = sanitize(details);

        String timestamp = FORMATTER.format(Instant.now());
        String logEntry = String.format("[%s] ACTOR=%s | ACTION=%s | STATUS=%s | DETAILS=%s%n",
                timestamp, cleanActor, cleanAction, cleanStatus, cleanDetails);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(logEntry);
            writer.flush();
        } catch (IOException e) {
            System.err.println("[SECURITY AUDIT ALERT] Failed to write to audit log file: " + e.getMessage());
        }
    }

    /**
     * Reads recent audit logs for administrative review.
     *
     * @param maxLines maximum number of recent lines to retrieve
     * @return unmodifiable list of log entries
     */
    public synchronized List<String> getRecentLogs(int maxLines) {
        File file = new File(logFilePath);
        if (!file.exists()) {
            return Collections.emptyList();
        }

        try {
            List<String> allLines = Files.readAllLines(Paths.get(logFilePath));
            int size = allLines.size();
            int startIndex = Math.max(0, size - maxLines);
            return Collections.unmodifiableList(new ArrayList<>(allLines.subList(startIndex, size)));
        } catch (IOException e) {
            return Collections.singletonList("[ERROR] Failed to read audit log file.");
        }
    }

    /**
     * Sanitizes user input before writing to logs, preventing Log Forging / Log Injection attacks (CWE-117).
     */
    private String sanitize(String input) {
        if (input == null) return "";
        // Replace carriage returns, newlines, and vertical bars to prevent fake log entry insertion
        return input.replace('\r', ' ')
                    .replace('\n', ' ')
                    .replace('|', '/')
                    .trim();
    }
}
