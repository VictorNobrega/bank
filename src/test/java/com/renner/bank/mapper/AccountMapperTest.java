package com.renner.bank.mapper;

import com.renner.bank.domain.Account;
import com.renner.bank.dto.AccountResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    private final AccountMapper accountMapper = new AccountMapper();

    @Test
    void shouldMapAccountToResponse() {
        UUID accountId = UUID.randomUUID();
        Account account = new Account("João Silva", new BigDecimal("1000.00"));
        account.setId(accountId);

        AccountResponse response = accountMapper.toResponse(account);

        assertThat(response.id()).isEqualTo(accountId);
        assertThat(response.name()).isEqualTo("João Silva");
        assertThat(response.balance()).isEqualByComparingTo("1000.00");
    }
}
