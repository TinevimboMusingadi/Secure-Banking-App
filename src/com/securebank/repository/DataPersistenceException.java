package com.securebank.repository;

/**
 * Exception thrown when file read, write, or parsing failures occur in data persistence.
 * Prevents exposing OS file paths or system-level IO traces to end users.
 *
 * @author Tinevimbo Musingadi (H250125B)
 */
public class DataPersistenceException extends Exception {

    public DataPersistenceException(String message) {
        super(message);
    }

    public DataPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
