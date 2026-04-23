package com.renner.bank.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleAccountNotFound() {
        var accountId = UUID.randomUUID();

        var problem = handler.handleAccountNotFound(new AccountNotFoundException(accountId));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).contains(accountId.toString());
    }

    @Test
    void shouldHandleTransferNotFound() {
        var transferId = UUID.randomUUID();

        var problem = handler.handleTransferNotFound(new TransferNotFoundException(transferId));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).contains(transferId.toString());
    }

    @Test
    void shouldHandleInsufficientFunds() {
        var accountId = UUID.randomUUID();

        var problem = handler.handleInsufficientFunds(
                new InsufficientFundsException(accountId, new BigDecimal("10.00"), new BigDecimal("20.00"))
        );

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getDetail()).contains(accountId.toString());
    }

    @Test
    void shouldHandleTransferToSameAccount() {
        var problem = handler.handleSameAccount(new TransferToSameAccountException());

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Source and destination accounts must be different");
    }

    @Test
    void shouldBuildValidationProblemFromBindException() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "Nome é obrigatório"));
        bindingResult.addError(new FieldError("request", "amount", null));

        var problem = handler.handleBindValidation(new BindException(bindingResult));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
        assertThat(problem.getProperties())
                .containsEntry("errors", java.util.Map.of("name", "Nome é obrigatório", "amount", "invalid"));
    }
}
