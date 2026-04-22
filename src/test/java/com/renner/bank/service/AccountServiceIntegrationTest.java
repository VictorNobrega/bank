package com.renner.bank.service;

import com.renner.bank.domain.Account;
import com.renner.bank.dto.AccountRequest;
import com.renner.bank.dto.AccountResponse;
import com.renner.bank.dto.TransferRequest;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.dto.pagination.PaginatedResponse;
import com.renner.bank.exception.AccountNotFoundException;
import com.renner.bank.repository.AccountRepository;
import com.renner.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceIntegrationTest {

    @Autowired AccountService accountService;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired TransactionService transactionService;
    @MockitoBean NotificationService notificationService;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldCreateAndPersistAccount() {
        AccountResponse response = accountService.create(new AccountRequest("Alice", new BigDecimal("1000.00")));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Alice");
        assertThat(response.balance()).isEqualByComparingTo("1000.00");
        assertThat(accountRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldFindAccountById() {
        AccountResponse created = accountService.create(new AccountRequest("Bob", new BigDecimal("500.00")));

        AccountResponse found = accountService.findById(created.id());

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo("Bob");
        assertThat(found.balance()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> accountService.findById(randomId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(randomId.toString());
    }

    @Test
    void shouldListAccountsOrderedByName() {
        accountService.create(new AccountRequest("Zara", new BigDecimal("100.00")));
        accountService.create(new AccountRequest("Alice", new BigDecimal("200.00")));
        accountService.create(new AccountRequest("Marco", new BigDecimal("300.00")));

        PaginatedResponse<AccountResponse> response = accountService.findAll(0, 20);

        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.content()).extracting(AccountResponse::name)
                .containsExactly("Alice", "Marco", "Zara");
    }

    @Test
    void shouldReturnPaginatedAccounts() {
        accountService.create(new AccountRequest("Alice", new BigDecimal("100.00")));
        accountService.create(new AccountRequest("Bob", new BigDecimal("200.00")));
        accountService.create(new AccountRequest("Carla", new BigDecimal("300.00")));

        PaginatedResponse<AccountResponse> page1 = accountService.findAll(0, 2);
        PaginatedResponse<AccountResponse> page2 = accountService.findAll(1, 2);

        assertThat(page1.content()).hasSize(2);
        assertThat(page1.totalElements()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);
        assertThat(page2.content()).hasSize(1);
    }

    @Test
    void shouldCheckExistenceById() {
        AccountResponse created = accountService.create(new AccountRequest("Carla", new BigDecimal("100.00")));

        assertThat(accountService.existsById(created.id())).isTrue();
        assertThat(accountService.existsById(UUID.randomUUID())).isFalse();
    }

    @Test
    void shouldReturnStatementForAccount() {
        Account source = accountRepository.save(new Account("Alice", new BigDecimal("1000.00")));
        Account destination = accountRepository.save(new Account("Bob", new BigDecimal("500.00")));

        transactionService.transfer(new TransferRequest(source.getId(), destination.getId(), new BigDecimal("100.00")));
        transactionService.transfer(new TransferRequest(source.getId(), destination.getId(), new BigDecimal("50.00")));

        PaginatedResponse<TransactionResponse> statement = accountService.getStatement(source.getId(), 0, 10);

        assertThat(statement.totalElements()).isEqualTo(2);
        assertThat(statement.content()).allMatch(tx -> tx.status().equals("COMPLETED"));
    }
}
