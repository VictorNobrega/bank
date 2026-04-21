package com.renner.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Transfer request")
public record TransferRequest(

        @NotNull(message = "O campo `sourceAccountId` é obrigatório.")
        @Schema(description = "Source account ID")
        UUID sourceAccountId,

        @NotNull(message = "O campo `destinationAccountId` é obrigatório.")
        @Schema(description = "Destination account ID")
        UUID destinationAccountId,

        @NotNull(message = "O campo `amount` é obrigatório.")
        @DecimalMin(value = "0.01", message = "O campo `amount` deve ser maior ou igual a 0.01.")
        @Schema(description = "Amount to transfer", example = "100.00")
        BigDecimal amount
) {}
