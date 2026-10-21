package com.zainabtaha.banking.model;

public enum TransactionType {
    OPENING_DEPOSIT("Opening deposit", true),
    DEPOSIT("Deposit", true),
    WITHDRAWAL("Withdrawal", false),
    TRANSFER_IN("Transfer in", true),
    TRANSFER_OUT("Transfer out", false),
    INTEREST("Interest", true);

    private final String label;
    private final boolean credit;

    TransactionType(String label, boolean credit) {
        this.label = label;
        this.credit = credit;
    }

    public String label() {
        return label;
    }

    /** True if this transaction adds money to the account. */
    public boolean isCredit() {
        return credit;
    }

    @Override
    public String toString() {
        return label;
    }
}
