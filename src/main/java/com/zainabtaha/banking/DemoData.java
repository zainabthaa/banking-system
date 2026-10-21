package com.zainabtaha.banking;

import com.zainabtaha.banking.model.AccountType;
import com.zainabtaha.banking.repository.AccountRepository;
import com.zainabtaha.banking.service.AdminService;
import com.zainabtaha.banking.service.OpenAccountRequest;
import java.math.BigDecimal;

/**
 * Creates two sample accounts on first launch so the app can be explored straight away.
 * The PINs are documented in the README; they are hashed like any other PIN.
 */
final class DemoData {

    static final String SAVINGS_NUMBER = "1000000001";
    static final String CHECKING_NUMBER = "1000000002";

    private DemoData() {
    }

    static void seedIfEmpty(AccountRepository accounts, AdminService admin) {
        if (!accounts.findAll().isEmpty()) {
            return;
        }
        admin.openAccount(new OpenAccountRequest(SAVINGS_NUMBER, "Sara Ahmed", "1234".toCharArray(),
                AccountType.SAVINGS, new BigDecimal("2500.00"), new BigDecimal("2.5")));
        admin.openAccount(new OpenAccountRequest(CHECKING_NUMBER, "Omar Khalid", "4321".toCharArray(),
                AccountType.CHECKING, new BigDecimal("300.00"), new BigDecimal("500.00")));
    }
}
