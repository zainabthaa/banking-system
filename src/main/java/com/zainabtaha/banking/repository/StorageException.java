package com.zainabtaha.banking.repository;

import com.zainabtaha.banking.model.BankingException;

/** A file could not be read or written. Wraps {@link java.io.IOException} at the storage boundary. */
public class StorageException extends BankingException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
