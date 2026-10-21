package com.zainabtaha.banking.model;

import java.math.BigDecimal;

public class InsufficientFundsException extends BankingException {

    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientFundsException(String accountNumber, BigDecimal requested, BigDecimal available) {
        super("Insufficient funds in account " + accountNumber
                + ": requested " + requested.toPlainString()
                + ", available " + available.toPlainString() + ".");
        this.requested = requested;
        this.available = available;
    }

    public BigDecimal requested() {
        return requested;
    }

    public BigDecimal available() {
        return available;
    }
}
