package com.securebank.security;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Strict whitelist-based input validator and sanitizer.
 * Protects against:
 * 1. SQL / NoSQL / Command Injection.
 * 2. Delimiter Injection (CSV/text-file delimiter corruption via pipe, newline, or carriage return).
 * 3. Buffer overflows / excessive memory allocation.
 * 4. Numeric boundary issues and floating point precision exploitation.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public final class InputValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']{2,50}$");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^ACC-\\d{6}$");
    private static final Pattern DELIMITER_INJECTION_PATTERN = Pattern.compile("[|\\r\\n\\t]");

    // Password must contain >= 8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special symbol
    private static final Pattern PASSWORD_STRENGTH_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+\\-=])[A-Za-z\\d@$!%*?&#^()_+\\-=]{8,64}$");

    private static final BigDecimal MAX_TRANSACTION_LIMIT = new BigDecimal("1000000.00");
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");

    private InputValidator() {
        // Utility class
    }

    /**
     * Validates alphanumeric username format.
     */
    public static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    /**
     * Validates real person name format.
     */
    public static boolean isValidFullName(String fullName) {
        if (fullName == null) return false;
        return FULL_NAME_PATTERN.matcher(fullName.trim()).matches();
    }

    /**
     * Enforces password complexity policy.
     */
    public static boolean isStrongPassword(char[] password) {
        if (password == null || password.length < 8 || password.length > 64) {
            return false;
        }
        String pwdStr = new String(password);
        return PASSWORD_STRENGTH_PATTERN.matcher(pwdStr).matches();
    }

    /**
     * Validates standardized account number format.
     */
    public static boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null) return false;
        return ACCOUNT_NUMBER_PATTERN.matcher(accountNumber.trim()).matches();
    }

    /**
     * Validates financial transaction amount (positive, non-zero, max 2 decimal places, under limit).
     */
    public static boolean isValidAmount(BigDecimal amount) {
        if (amount == null) return false;
        if (amount.scale() > 2) return false;
        return amount.compareTo(MIN_TRANSACTION_AMOUNT) >= 0 && amount.compareTo(MAX_TRANSACTION_LIMIT) <= 0;
    }

    /**
     * Checks if input contains delimiter or newline characters that could break text file persistence.
     */
    public static boolean containsDelimiterInjection(String input) {
        if (input == null) return false;
        return DELIMITER_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Sanitizes general text strings by stripping leading/trailing whitespace and control chars.
     */
    public static String sanitizeString(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }
}
