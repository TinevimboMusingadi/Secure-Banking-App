package com.securebank.service;

/**
 * Custom checked exception for banking domain errors.
 * Ensures internal technical stack traces or database errors are not leaked
 * to the end-user, mitigating Information Disclosure vulnerabilities.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class BankingException extends Exception {

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
