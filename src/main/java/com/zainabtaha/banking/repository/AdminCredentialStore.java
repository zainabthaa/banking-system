package com.zainabtaha.banking.repository;

import java.util.Optional;

/** Stores the administrator's password hash (never the password itself). */
public interface AdminCredentialStore {

    Optional<String> loadPasswordHash();

    void savePasswordHash(String hash);
}
