package com.renner.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Account creation request")
public record AccountRequest(

        @NotBlank(message = "O campo `name` é obrigatório.")
        @Schema(description = "Account holder name", example = "João Silva")
        String name,

        @NotNull(message = "O campo `initialBalance` é obrigatório.")
        @DecimalMin(value = "0.00", message = "O campo `initialBalance` não pode ser negativo.")
        @Schema(description = "Initial balance", example = "1000.00")
        BigDecimal initialBalance
) {}
