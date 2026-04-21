package com.renner.bank.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldReturnCustomMessageWhenNameIsBlank() {
        AccountRequest request = new AccountRequest(" ", new BigDecimal("100.00"));

        Set<ConstraintViolation<AccountRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O campo `name` é obrigatório.");
    }

    @Test
    void shouldReturnCustomMessageWhenInitialBalanceIsNull() {
        AccountRequest request = new AccountRequest("João Silva", null);

        Set<ConstraintViolation<AccountRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O campo `initialBalance` é obrigatório.");
    }

    @Test
    void shouldReturnCustomMessageWhenInitialBalanceIsNegative() {
        AccountRequest request = new AccountRequest("João Silva", new BigDecimal("-0.01"));

        Set<ConstraintViolation<AccountRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("O campo `initialBalance` não pode ser negativo.");
    }
}
