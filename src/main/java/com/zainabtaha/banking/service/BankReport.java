package com.zainabtaha.banking.service;

import java.math.BigDecimal;

/** Summary figures shown on the admin dashboard. */
public record BankReport(
        int totalAccounts,
        int lockedAccounts,
        BigDecimal totalHoldings,
        int depositCount,
        BigDecimal depositTotal,
        int withdrawalCount,
        BigDecimal withdrawalTotal,
        int transferCount,
        BigDecimal transferTotal,
        BigDecimal interestPaid) {
}
