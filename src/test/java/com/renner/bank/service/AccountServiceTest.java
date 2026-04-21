package com.renner.bank.service;

import com.renner.bank.domain.Account;
import com.renner.bank.dto.AccountRequest;
import com.renner.bank.dto.AccountResponse;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.dto.pagination.PaginatedResponse;
import com.renner.bank.exception.AccountNotFoundException;
import com.renner.bank.mapper.AccountMapper;
import com.renner.bank.mapper.PageMapper;
import com.renner.bank.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock private AccountRepository accountRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private PageMapper pageMapper;
    @Mock private TransactionService transactionService;
    @InjectMocks private AccountService accountService;

    private UUID accountId;
    private Account account;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        account = new Account("João Silva", new BigDecimal("1000.00"));
        account.setId(accountId);
    }

    @Test
    void shouldCreateAccount() {
        AccountRequest request = new AccountRequest("João Silva", new BigDecimal("1000.00"));
        AccountResponse mappedResponse = new AccountResponse(accountId, "João Silva", new BigDecimal("1000.00"));
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(mappedResponse);

        AccountResponse response = accountService.create(request);

        assertThat(response.name()).isEqualTo("João Silva");
        assertThat(response.balance()).isEqualByComparingTo("1000.00");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void shouldFindAccountById() {
        AccountResponse mappedResponse = new AccountResponse(accountId, "João Silva", new BigDecimal("1000.00"));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(mappedResponse);

        AccountResponse response = accountService.findById(accountId);

        assertThat(response.id()).isEqualTo(accountId);
        assertThat(response.name()).isEqualTo("João Silva");
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(accountId.toString());
    }

    @Test
    void shouldFindAccountByIdWithLock() {
        when(accountRepository.findByIdWithLock(accountId)).thenReturn(Optional.of(account));

        Account response = accountService.findByIdWithLock(accountId);

        assertThat(response).isSameAs(account);
    }

    @Test
    void shouldThrowWhenAccountWithLockNotFound() {
        when(accountRepository.findByIdWithLock(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findByIdWithLock(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(accountId.toString());
    }

    @Test
    void shouldListAllAccounts() {
        Account another = new Account("Maria Costa", new BigDecimal("2000.00"));
        another.setId(UUID.randomUUID());
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<Account> accountPage = new PageImpl<>(List.of(account, another), pageable, 2);
        PaginatedResponse<AccountResponse> paginatedResponse = new PaginatedResponse<>(
                List.of(
                        new AccountResponse(account.getId(), account.getName(), account.getBalance()),
                        new AccountResponse(another.getId(), another.getName(), another.getBalance())
                ),
                2,
                1,
                2,
                0,
                20
        );

        when(pageMapper.buildPagination(0, 20)).thenReturn(pageable);
        when(accountRepository.findAllByOrderByNameAsc(pageable)).thenReturn(accountPage);
        when(accountMapper.toResponse(account))
                .thenReturn(new AccountResponse(account.getId(), account.getName(), account.getBalance()));
        when(accountMapper.toResponse(another))
                .thenReturn(new AccountResponse(another.getId(), another.getName(), another.getBalance()));
        when(pageMapper.toPaginatedResponse(org.mockito.ArgumentMatchers.<Page<AccountResponse>>any()))
                .thenReturn(paginatedResponse);

        PaginatedResponse<AccountResponse> responses = accountService.findAll(0, 20);

        assertThat(responses.totalElements()).isEqualTo(2);
        assertThat(responses.content()).hasSize(2);
        assertThat(responses.content()).extracting(AccountResponse::name)
                .containsExactly("João Silva", "Maria Costa");
    }

    @Test
    void shouldDelegateStatementLookupToTransferService() {
        PaginatedResponse<TransactionResponse> statementResponse = new PaginatedResponse<>(List.of(), 0, 0, 0, 0, 20);
        when(transactionService.getStatement(accountId, 0, 20)).thenReturn(statementResponse);

        PaginatedResponse<TransactionResponse> statement = accountService.getStatement(accountId, 0, 20);

        assertThat(statement).isSameAs(statementResponse);
        verify(transactionService).getStatement(accountId, 0, 20);
    }

    @Test
    void shouldDelegateExistsByIdToRepository() {
        when(accountRepository.existsById(accountId)).thenReturn(true);

        boolean exists = accountService.existsById(accountId);

        assertThat(exists).isTrue();
        verify(accountRepository).existsById(accountId);
    }
}
