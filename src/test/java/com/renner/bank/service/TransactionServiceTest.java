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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationService notificationService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private PageMapper pageMapper;
    @Mock private TransactionMapper transactionMapper;
    @Mock private AccountService accountService;

    private TransactionService transactionService;

    private UUID sourceId;
    private UUID destinationId;
    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        destinationId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        source = new Account("Alice", new BigDecimal("1000.00"));
        source.setId(sourceId);

        destination = new Account("Bob", new BigDecimal("500.00"));
        destination.setId(destinationId);

        transactionService = new TransactionService(
                transactionRepository,
                notificationService,
                transactionManager,
                pageMapper,
                transactionMapper,
                accountService
        );
    }

    @Test
    void shouldTransferSuccessfully() {
        var request = new TransferRequest(sourceId, destinationId, new BigDecimal("200.00"));
        var createdTransaction = new Transaction(
                source,
                destination,
                new BigDecimal("200.00"),
                TransactionStatus.COMPLETED,
                LocalDateTime.now()
        );
        var mappedResponse = new TransactionResponse(
                UUID.randomUUID(),
                sourceId,
                "Alice",
                destinationId,
                "Bob",
                new BigDecimal("200.00"),
                "COMPLETED",
                LocalDateTime.now()
        );
        when(accountService.findByIdWithLock(sourceId)).thenReturn(source);
        when(accountService.findByIdWithLock(destinationId)).thenReturn(destination);
        when(transactionMapper.createTransaction(source, destination, new BigDecimal("200.00"), TransactionStatus.COMPLETED))
                .thenReturn(createdTransaction);
        when(transactionRepository.save(createdTransaction)).thenReturn(createdTransaction);
        when(transactionMapper.toResponse(createdTransaction)).thenReturn(mappedResponse);

        var response = transactionService.transfer(request);

        assertThat(response.sourceAccountId()).isEqualTo(sourceId);
        assertThat(response.destinationAccountId()).isEqualTo(destinationId);
        assertThat(response.amount()).isEqualByComparingTo("200.00");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(source.getBalance()).isEqualByComparingTo("800.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("700.00");
        verify(notificationService).notify(sourceId, "Alice", destinationId, "Bob", new BigDecimal("200.00"));
    }

    @Test
    void shouldReturnStatementWhenAccountExists() {
        var pageable = PageRequest.of(0, 20);
        var transaction = new Transaction(
                source,
                destination,
                new BigDecimal("150.00"),
                TransactionStatus.COMPLETED,
                LocalDateTime.now()
        );
        var page = new PageImpl<>(List.of(transaction), pageable, 1);
        var mappedResponse = new TransactionResponse(
                UUID.randomUUID(),
                sourceId,
                "Alice",
                destinationId,
                "Bob",
                new BigDecimal("150.00"),
                "COMPLETED",
                transaction.getCreatedAt()
        );
        var paginatedResponse = new PaginatedResponse<>(
                List.of(mappedResponse),
                1,
                1,
                1,
                0,
                20
        );

        when(accountService.existsById(sourceId)).thenReturn(true);
        when(pageMapper.buildPagination(eq(0), eq(20), any(Sort.class))).thenReturn(pageable);
        when(transactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(transactionMapper.toResponse(transaction)).thenReturn(mappedResponse);
        when(pageMapper.toPaginatedResponse(org.mockito.ArgumentMatchers.<Page<TransactionResponse>>any()))
                .thenReturn(paginatedResponse);

        var response = transactionService.getTransactionByAccountId(sourceId, 0, 20);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.sourceAccountId()).isEqualTo(sourceId);
            assertThat(item.destinationAccountId()).isEqualTo(destinationId);
            assertThat(item.amount()).isEqualByComparingTo("150.00");
        });
    }

    @Test
    void shouldThrowWhenStatementAccountDoesNotExist() {
        when(accountService.existsById(sourceId)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.getTransactionByAccountId(sourceId, 0, 20))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(sourceId.toString());
    }

    @Test
    void shouldFindTransferById() {
        var transferId = UUID.randomUUID();
        var transaction = new Transaction(
                source,
                destination,
                new BigDecimal("200.00"),
                TransactionStatus.COMPLETED,
                LocalDateTime.now()
        );
        var mappedResponse = new TransactionResponse(
                transferId,
                sourceId,
                "Alice",
                destinationId,
                "Bob",
                new BigDecimal("200.00"),
                "COMPLETED",
                transaction.getCreatedAt()
        );

        when(transactionRepository.findById(transferId)).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(mappedResponse);

        var response = transactionService.findById(transferId);

        assertThat(response.sourceAccountId()).isEqualTo(sourceId);
        assertThat(response.sourceAccountName()).isEqualTo("Alice");
        assertThat(response.destinationAccountId()).isEqualTo(destinationId);
        assertThat(response.destinationAccountName()).isEqualTo("Bob");
        assertThat(response.amount()).isEqualByComparingTo("200.00");
    }

    @Test
    void shouldThrowWhenTransferNotFound() {
        var transferId = UUID.randomUUID();
        when(transactionRepository.findById(transferId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(transferId))
                .isInstanceOf(TransferNotFoundException.class)
                .hasMessageContaining(transferId.toString());
    }

    @Test
    void shouldThrowOnInsufficientFundsAndPersistFailedTransaction() {
        var request = new TransferRequest(sourceId, destinationId, new BigDecimal("5000.00"));
        var failedTransaction = new Transaction(
                source,
                destination,
                new BigDecimal("5000.00"),
                TransactionStatus.FAILED,
                LocalDateTime.now()
        );
        when(accountService.findByIdWithLock(sourceId)).thenReturn(source);
        when(accountService.findByIdWithLock(destinationId)).thenReturn(destination);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        when(transactionMapper.createTransaction(source, destination, new BigDecimal("5000.00"), TransactionStatus.FAILED))
                .thenReturn(failedTransaction);
        when(transactionRepository.save(failedTransaction)).thenReturn(failedTransaction);

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(source.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("500.00");
        verify(transactionMapper).createTransaction(source, destination, new BigDecimal("5000.00"), TransactionStatus.FAILED);
        verify(transactionRepository).save(failedTransaction);
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldThrowOnTransferToSameAccountAndPersistFailedTransaction() {
        var request = new TransferRequest(sourceId, sourceId, new BigDecimal("100.00"));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(TransferToSameAccountException.class);

        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldThrowWhenSourceAccountNotFound() {
        var request = new TransferRequest(sourceId, destinationId, new BigDecimal("100.00"));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        when(accountService.findByIdWithLock(sourceId)).thenThrow(new AccountNotFoundException(sourceId));

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(sourceId.toString());

        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldThrowWhenDestinationAccountNotFound() {
        var request = new TransferRequest(sourceId, destinationId, new BigDecimal("100.00"));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        when(accountService.findByIdWithLock(sourceId)).thenReturn(source);
        when(accountService.findByIdWithLock(destinationId)).thenThrow(new AccountNotFoundException(destinationId));

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(destinationId.toString());

        verify(transactionRepository, never()).save(any(Transaction.class));
        verifyNoInteractions(notificationService);
    }
}
