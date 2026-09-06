package com.securebank.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Robust, cryptographically secure password hashing service using PBKDF2WithHmacSHA256.
 *
 * Secure Coding Features:
 * 1. Cryptographically strong pseudo-random number generator (SecureRandom) for salts.
 * 2. High iteration count (65,536) to defend against brute-force and GPU cracking.
 * 3. 256-bit derived key length.
 * 4. Constant-time array comparison (MessageDigest.isEqual) to eliminate timing attack vectors.
 * 5. Memory sanitization (zeroing out sensitive char arrays).
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Utility class: prevent instantiation
    }

    /**
     * Generates a secure random 16-byte salt.
     *
     * @return hex-encoded salt string
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Hashes a password with a provided hex-encoded salt.
     *
     * @param password char array of password (zeroed out after hashing)
     * @param saltHex  hex-encoded salt
     * @return hex-encoded hashed password
     */
    public static String hashPassword(char[] password, String saltHex) {
        if (password == null || password.length == 0 || saltHex == null || saltHex.isEmpty()) {
            throw new IllegalArgumentException("Password and salt must not be empty");
        }

        byte[] salt = hexToBytes(saltHex);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new SecurityException("Fatal cryptographic failure during password hashing", e);
        } finally {
            spec.clearPassword();
            // Secure memory wipe of the plaintext password array
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Verifies whether a candidate plaintext password matches the stored hash and salt.
     * Uses constant-time equality check to prevent timing side-channel attacks.
     *
     * @param candidatePassword candidate password char array
     * @param storedSaltHex     stored hex salt
     * @param storedHashHex     stored hex hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(char[] candidatePassword, String storedSaltHex, String storedHashHex) {
        if (candidatePassword == null || storedSaltHex == null || storedHashHex == null) {
            return false;
        }

        try {
            String candidateHashHex = hashPassword(candidatePassword, storedSaltHex);
            byte[] candidateHash = hexToBytes(candidateHashHex);
            byte[] storedHash = hexToBytes(storedHashHex);

            // Constant-time comparison
            return MessageDigest.isEqual(candidateHash, storedHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string length");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
