package com.renner.bank.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record NotificationRequest(
        UUID sourceId,
        String sourceName,
        UUID destinationId,
        String destinationName,
        BigDecimal amount
) {}
