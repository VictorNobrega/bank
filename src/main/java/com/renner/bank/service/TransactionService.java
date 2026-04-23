package com.renner.bank.service;

import com.renner.bank.domain.Account;
import com.renner.bank.domain.Transaction;
import com.renner.bank.domain.TransactionStatus;
import com.renner.bank.dto.TransferRequest;
import com.renner.bank.dto.TransactionResponse;
import com.renner.bank.dto.pagination.PaginatedResponse;
import com.renner.bank.exception.AccountNotFoundException;
import com.renner.bank.exception.InsufficientFundsException;
import com.renner.bank.exception.TransferNotFoundException;
import com.renner.bank.exception.TransferToSameAccountException;
import com.renner.bank.mapper.PageMapper;
import com.renner.bank.mapper.TransactionMapper;
import com.renner.bank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    public static final int INSUFFICIENT_FUNDS = 0;

    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final PageMapper pageMapper;
    private final TransactionMapper transactionMapper;
    private final AccountService accountService;

    public TransactionService(TransactionRepository transactionRepository,
                              NotificationService notificationService,
                              PlatformTransactionManager transactionManager,
                              PageMapper pageMapper,
                              TransactionMapper transactionMapper,
                              @Lazy AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
        this.pageMapper = pageMapper;
        this.transactionMapper = transactionMapper;
        this.accountService = accountService;
    }

    public PaginatedResponse<TransactionResponse> getTransactionByAccountId(UUID accountId,
                                                                            Integer page,
                                                                            Integer size) {
        if (!accountService.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        var pageable = pageMapper.buildPagination(page, size);
        var statementPage = transactionRepository.findByAccountId(accountId, pageable)
                .map(transactionMapper::toResponse);
        return pageMapper.toPaginatedResponse(statementPage);
    }

    public PaginatedResponse<TransactionResponse> findAll(Integer page, Integer size) {
        var pageable = pageMapper.buildPagination(page, size);
        var transactionPage = transactionRepository.findAllWithAccounts(pageable)
                .map(transactionMapper::toResponse);
        return pageMapper.toPaginatedResponse(transactionPage);
    }

    public TransactionResponse findById(UUID transferId) {
        return transactionRepository.findById(transferId)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        var sourceId = request.sourceAccountId();
        var destinationId = request.destinationAccountId();
        Account source      = null;
        Account destination = null;

        log.info("Transfer requested: {} -> {}, amount={}", sourceId, destinationId, request.amount());

        try {
            validateDistinctAccounts(sourceId, destinationId);

            var locked = findAccountsWithOrderedLocks(sourceId, destinationId);
            source = locked[0];
            destination = locked[1];

            applyFundsTransfer(source, destination, request.amount());

            var transaction = persistCompletedTransaction(source, destination, request.amount());

            notifyTransferParticipants(source, destination, request.amount());

            return transactionMapper.toResponse(transaction);
        } catch (RuntimeException ex) {
            saveFailedTransaction(source, destination, request.amount());
            throw ex;
        }
    }

    private void validateDistinctAccounts(UUID sourceId, UUID destinationId) {
        if (sourceId.equals(destinationId)) {
            throw new TransferToSameAccountException();
        }
    }

    private void applyFundsTransfer(Account source, Account destination, BigDecimal amount) {
        if (source.getBalance().compareTo(amount) < INSUFFICIENT_FUNDS) {
            throw new InsufficientFundsException(source.getId(), source.getBalance(), amount);
        }
        source.debit(amount);
        destination.credit(amount);
    }

    private Transaction persistCompletedTransaction(Account source, Account destination, BigDecimal amount) {
        var transaction = transactionMapper.createTransaction(
                source,
                destination,
                amount,
                TransactionStatus.COMPLETED
        );
        transaction = transactionRepository.save(transaction);
        log.info("Transfer completed: transactionId={}", transaction.getId());
        return transaction;
    }

    private void notifyTransferParticipants(Account source, Account destination, BigDecimal amount) {
        notificationService.notify(
                source.getId(), source.getName(),
                destination.getId(), destination.getName(),
                amount
        );
    }

    private void saveFailedTransaction(Account source,
                                       Account destination,
                                       BigDecimal amount) {
        transactionTemplate.execute(status -> {
            if (Objects.isNull(source) || Objects.isNull(destination)) {
                log.warn("Failed transaction could not be persisted because source or destination account was not resolved");
                return null;
            }

            var failed = transactionMapper.createTransaction(
                    source,
                    destination,
                    amount,
                    TransactionStatus.FAILED
            );
            return transactionRepository.save(failed);
        });
    }

    private Account[] findAccountsWithOrderedLocks(UUID sourceId, UUID destinationId) {
        if (sourceId.compareTo(destinationId) < 0) {
            var first = accountService.findByIdWithLock(sourceId);
            var second = accountService.findByIdWithLock(destinationId);
            return new Account[]{first, second};
        }
        var first = accountService.findByIdWithLock(destinationId);
        var second = accountService.findByIdWithLock(sourceId);
        return new Account[]{second, first};
    }
}
