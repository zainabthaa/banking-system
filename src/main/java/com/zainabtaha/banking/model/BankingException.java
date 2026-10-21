package com.zainabtaha.banking.model;

/**
 * Base type for every business-rule failure (bad credentials, insufficient funds, ...).
 * The UI catches this one type and shows its message to the user; anything else is a bug.
 */
public class BankingException extends RuntimeException {

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
