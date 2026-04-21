package com.renner.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Account data")
public record AccountResponse(
        UUID id,
        String name,
        BigDecimal balance
) {}
