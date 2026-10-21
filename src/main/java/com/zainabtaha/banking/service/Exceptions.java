package com.zainabtaha.banking.service;

import com.zainabtaha.banking.model.BankingException;

/** Business-rule failures raised by the service layer. */
public final class Exceptions {

    private Exceptions() {
    }

    public static class AuthenticationException extends BankingException {
        public AuthenticationException(String message) {
            super(message);
        }
    }

    public static class AccountLockedException extends BankingException {
        public AccountLockedException(String accountNumber) {
            super("Account " + accountNumber + " is locked after too many failed attempts. "
                    + "Please contact an administrator.");
        }
    }

    public static class AccountNotFoundException extends BankingException {
        public AccountNotFoundException(String accountNumber) {
            super("Account " + accountNumber + " was not found.");
        }
    }

    public static class DuplicateAccountException extends BankingException {
        public DuplicateAccountException(String accountNumber) {
            super("Account number " + accountNumber + " is already in use.");
        }
    }
}
