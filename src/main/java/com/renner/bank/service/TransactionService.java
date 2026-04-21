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
import org.springframework.data.domain.Page;
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

    public PaginatedResponse<TransactionResponse> getStatement(UUID accountId,
                                                               Integer page,
                                                               Integer size) {
        if (!accountService.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        var pageable = pageMapper.buildPagination(page, size);
        Page<TransactionResponse> statementPage = transactionRepository.findByAccountId(accountId, pageable)
                .map(transactionMapper::toResponse);
        return pageMapper.toPaginatedResponse(statementPage);
    }

    public TransactionResponse findById(UUID transferId) {
        return transactionRepository.findById(transferId)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new TransferNotFoundException(transferId));
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        UUID sourceId      = request.sourceAccountId();
        UUID destinationId = request.destinationAccountId();
        Account source      = null;
        Account destination = null;

        log.info("Transfer requested: {} -> {}, amount={}", sourceId, destinationId, request.amount());

        try {
            validateDistinctAccounts(sourceId, destinationId);

            UUID firstId = sourceId.compareTo(destinationId) < 0 ? sourceId : destinationId;
            UUID secondId = sourceId.compareTo(destinationId) < 0 ? destinationId : sourceId;

            Account first = findAccountWithLock(firstId);
            Account second = findAccountWithLock(secondId);

            source = firstId.equals(sourceId) ? first : second;
            destination = firstId.equals(destinationId) ? first : second;

            applyFundsTransfer(source, destination, request.amount());

            Transaction transaction = persistCompletedTransaction(source, destination, request.amount());

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
        Transaction transaction = transactionMapper.createTransaction(
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

            Transaction failed = transactionMapper.createTransaction(
                    source,
                    destination,
                    amount,
                    TransactionStatus.FAILED
            );
            return transactionRepository.save(failed);
        });
    }

    private Account findAccountWithLock(UUID sourceId) {
        return accountService.findByIdWithLock(sourceId);
    }
}
