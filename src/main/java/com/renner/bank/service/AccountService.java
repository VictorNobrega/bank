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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PageMapper pageMapper;
    private final TransactionService transactionService;

    public AccountService(AccountRepository accountRepository,
                          AccountMapper accountMapper,
                          PageMapper pageMapper,
                          TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.pageMapper = pageMapper;
        this.transactionService = transactionService;
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        Account account = new Account(request.name(), request.initialBalance());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    public Account findByIdWithLock(UUID id) {
        return accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public AccountResponse findById(UUID id) {
        return accountRepository.findById(id)
                .map(accountMapper::toResponse)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public PaginatedResponse<AccountResponse> findAll(Integer page, Integer size) {
        var pageable = pageMapper.buildPagination(page, size);
        Page<AccountResponse> accountPage = accountRepository.findAllByOrderByNameAsc(pageable)
                .map(accountMapper::toResponse);
        return pageMapper.toPaginatedResponse(accountPage);
    }

    public PaginatedResponse<TransactionResponse> getTransactionByAccountId(UUID accountId,
                                                                            Integer page,
                                                                            Integer size) {
        return transactionService.getTransactionByAccountId(accountId, page, size);
    }

    public boolean existsById(UUID accountId) {
        return accountRepository.existsById(accountId);
    }
}
