package com.zainabtaha.banking.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An immutable entry in an account's ledger.
 *
 * @param id           unique identifier (UUID)
 * @param accountNumber the account this entry belongs to
 * @param type         what happened
 * @param amount       always positive; {@link TransactionType#isCredit()} gives the direction
 * @param balanceAfter the account balance immediately after this transaction
 * @param timestamp    when it happened (UTC)
 * @param description  free text, e.g. the counterparty of a transfer
 */
public record Transaction(
        String id,
        String accountNumber,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant timestamp,
        String description) {

    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(accountNumber, "accountNumber");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(timestamp, "timestamp");
        amount = Money.of(Objects.requireNonNull(amount, "amount"));
        balanceAfter = Money.of(Objects.requireNonNull(balanceAfter, "balanceAfter"));
        description = description == null ? "" : description;
    }

    public static Transaction record(Account account, TransactionType type, BigDecimal amount,
                                     Instant when, String description) {
        return new Transaction(UUID.randomUUID().toString(), account.accountNumber(), type,
                amount, account.balance(), when, description);
    }

    /** The amount with a sign: positive for credits, negative for debits. */
    public BigDecimal signedAmount() {
        return type.isCredit() ? amount : amount.negate();
    }
}
