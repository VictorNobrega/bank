package com.renner.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Transaction result")
public record TransactionResponse(
        UUID transactionId,
        UUID sourceAccountId,
        String sourceAccountName,
        UUID destinationAccountId,
        String destinationAccountName,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {}
