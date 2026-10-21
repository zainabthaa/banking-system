package com.zainabtaha.banking.repository.file;

import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.model.TransactionType;
import com.zainabtaha.banking.repository.StorageException;
import com.zainabtaha.banking.repository.TransactionRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Append-only transaction ledger stored as CSV. */
public final class CsvTransactionRepository implements TransactionRepository {

    static final List<String> HEADER = List.of(
            "id", "account_number", "type", "amount", "balance_after", "timestamp", "description");

    private final Path file;
    private final List<Transaction> transactions = new ArrayList<>();

    public CsvTransactionRepository(Path file) {
        this.file = file;
        for (List<String> row : Csv.readRows(file)) {
            transactions.add(fromRow(row));
        }
    }

    @Override
    public synchronized void append(Transaction transaction) {
        appendAll(List.of(transaction));
    }

    @Override
    public synchronized void appendAll(List<Transaction> batch) {
        List<List<String>> rows = new ArrayList<>();
        for (Transaction t : batch) {
            rows.add(toRow(t));
        }
        Csv.append(file, HEADER, rows);
        transactions.addAll(batch);
    }

    @Override
    public synchronized List<Transaction> findByAccount(String accountNumber) {
        return transactions.stream()
                .filter(t -> t.accountNumber().equals(accountNumber))
                .toList();
    }

    @Override
    public synchronized List<Transaction> findAll() {
        return List.copyOf(transactions);
    }

    private static List<String> toRow(Transaction t) {
        return List.of(
                t.id(),
                t.accountNumber(),
                t.type().name(),
                t.amount().toPlainString(),
                t.balanceAfter().toPlainString(),
                t.timestamp().toString(),
                t.description());
    }

    private Transaction fromRow(List<String> r) {
        if (r.size() != HEADER.size()) {
            throw new StorageException("Corrupt row in " + file.getFileName(), null);
        }
        try {
            return new Transaction(
                    r.get(0),
                    r.get(1),
                    TransactionType.valueOf(r.get(2)),
                    new BigDecimal(r.get(3)),
                    new BigDecimal(r.get(4)),
                    Instant.parse(r.get(5)),
                    r.get(6));
        } catch (RuntimeException e) {
            throw new StorageException("Corrupt row in " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
