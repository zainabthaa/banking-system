package com.zainabtaha.banking.service;

import com.zainabtaha.banking.model.AccountType;
import java.math.BigDecimal;

/**
 * Everything an administrator supplies to open an account.
 *
 * @param rateOrLimit interest rate (%) for savings, overdraft limit for checking
 */
public record OpenAccountRequest(
        String accountNumber,
        String holderName,
        char[] pin,
        AccountType type,
        BigDecimal initialDeposit,
        BigDecimal rateOrLimit) {
}
