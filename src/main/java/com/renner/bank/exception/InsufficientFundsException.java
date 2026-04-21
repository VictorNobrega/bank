package com.renner.bank.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(UUID accountId, BigDecimal balance, BigDecimal requested) {
        super(String.format("Insufficient funds in account %s: balance=%s, requested=%s",
                accountId, balance, requested));
    }
}
