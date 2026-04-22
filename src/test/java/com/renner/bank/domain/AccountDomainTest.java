package com.renner.bank.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountDomainTest {

    @Test
    void shouldDebitBalance() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        account.debit(new BigDecimal("300.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void shouldCreditBalance() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        account.credit(new BigDecimal("500.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void shouldThrowWhenDebitingNullAmount() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> account.debit(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenDebitingZeroAmount() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> account.debit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenDebitingNegativeAmount() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> account.debit(new BigDecimal("-50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenDebitExceedsBalance() {
        Account account = new Account("Alice", new BigDecimal("100.00"));
        assertThatThrownBy(() -> account.debit(new BigDecimal("200.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldThrowWhenCreditingNullAmount() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> account.credit(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenCreditingZeroAmount() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> account.credit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenCreditingNegativeAmount() {
        Account account = new Account("Alice", new BigDecimal("1000.00"));
        assertThatThrownBy(() -> account.credit(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
