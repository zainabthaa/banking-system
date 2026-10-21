package com.zainabtaha.banking.repository.file;

import com.zainabtaha.banking.repository.AdminCredentialStore;
import com.zainabtaha.banking.repository.StorageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Keeps the admin password hash in a one-line text file. */
public final class FileAdminCredentialStore implements AdminCredentialStore {

    private final Path file;

    public FileAdminCredentialStore(Path file) {
        this.file = file;
    }

    @Override
    public Optional<String> loadPasswordHash() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8).strip();
            return content.isEmpty() ? Optional.empty() : Optional.of(content);
        } catch (IOException e) {
            throw new StorageException("Could not read admin credentials", e);
        }
    }

    @Override
    public void savePasswordHash(String hash) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, hash + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new StorageException("Could not save admin credentials", e);
        }
    }
}
