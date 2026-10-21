package com.zainabtaha.banking.repository.file;

import com.zainabtaha.banking.repository.StorageException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 CSV reading and writing, plus crash-safe file replacement.
 *
 * <p>The original version split lines on {@code ", "} and {@code ":"}, which broke as soon as a
 * customer name contained a comma. Quoting fields fixes that class of bug for good.
 */
final class Csv {

    private Csv() {
    }

    static String formatRow(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields.get(i)));
        }
        return sb.toString();
    }

    static String escape(String field) {
        String value = field == null ? "" : field;
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    static List<String> parseRow(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Unterminated quoted field");
        }
        fields.add(current.toString());
        return fields;
    }

    /** Reads all data rows (header skipped). Returns an empty list if the file does not exist. */
    static List<List<String>> readRows(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<List<String>> rows = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.isBlank()) {
                    rows.add(parseRow(line));
                }
            }
            return rows;
        } catch (IOException e) {
            throw new StorageException("Could not read " + file.getFileName(), e);
        }
    }

    /**
     * Writes the whole file to a temporary sibling and then renames it over the original.
     * A crash mid-write therefore leaves the previous version intact instead of a half-written file.
     */
    static void writeAtomically(Path file, List<String> header, List<List<String>> rows) {
        StringBuilder content = new StringBuilder(formatRow(header)).append('\n');
        for (List<String> row : rows) {
            content.append(formatRow(row)).append('\n');
        }
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, file.getFileName().toString(), ".tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Could not write " + file.getFileName(), e);
        }
    }

    /** Appends rows, writing the header first if the file is new. */
    static void append(Path file, List<String> header, List<List<String>> rows) {
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            StringBuilder content = new StringBuilder();
            if (!Files.exists(file) || Files.size(file) == 0) {
                content.append(formatRow(header)).append('\n');
            }
            for (List<String> row : rows) {
                content.append(formatRow(row)).append('\n');
            }
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new StorageException("Could not write " + file.getFileName(), e);
        }
    }
}
