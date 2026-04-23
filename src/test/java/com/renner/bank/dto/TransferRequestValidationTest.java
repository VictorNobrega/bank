package com.renner.bank.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldReturnCustomMessageWhenSourceAccountIdIsNull() {
        var request = new TransferRequest(null, UUID.randomUUID(), new BigDecimal("100.00"));

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O campo `sourceAccountId` é obrigatório.");
    }

    @Test
    void shouldReturnCustomMessageWhenDestinationAccountIdIsNull() {
        var request = new TransferRequest(UUID.randomUUID(), null, new BigDecimal("100.00"));

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O campo `destinationAccountId` é obrigatório.");
    }

    @Test
    void shouldReturnCustomMessageWhenAmountIsNull() {
        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), null);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O campo `amount` é obrigatório.");
    }

    @Test
    void shouldReturnCustomMessageWhenAmountIsBelowMinimum() {
        var request = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O campo `amount` deve ser maior ou igual a 0.01.");
    }
}
