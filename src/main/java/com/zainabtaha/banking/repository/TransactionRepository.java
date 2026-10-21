package com.zainabtaha.banking.repository;

import com.zainabtaha.banking.model.Transaction;
import java.util.List;

public interface TransactionRepository {

    void append(Transaction transaction);

    void appendAll(List<Transaction> transactions);

    /** All transactions for one account, oldest first. */
    List<Transaction> findByAccount(String accountNumber);

    /** Every transaction in the system, oldest first. */
    List<Transaction> findAll();
}
