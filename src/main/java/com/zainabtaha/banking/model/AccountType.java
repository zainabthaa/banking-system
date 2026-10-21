package com.zainabtaha.banking.model;

public enum AccountType {
    SAVINGS("Savings"),
    CHECKING("Checking");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
