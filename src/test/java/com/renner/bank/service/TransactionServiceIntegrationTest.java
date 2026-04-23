package com.renner.bank.service;

import com.renner.bank.domain.Account;
import com.renner.bank.domain.TransactionStatus;
import com.renner.bank.dto.TransferRequest;
import com.renner.bank.exception.AccountNotFoundException;
import com.renner.bank.exception.InsufficientFundsException;
import com.renner.bank.exception.TransferNotFoundException;
import com.renner.bank.exception.TransferToSameAccountException;
import com.renner.bank.repository.AccountRepository;
import com.renner.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceIntegrationTest {

    @Autowired TransactionService transactionService;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @MockitoBean NotificationService notificationService;

    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        source = accountRepository.save(new Account("Alice", new BigDecimal("1000.00")));
        destination = accountRepository.save(new Account("Bob", new BigDecimal("500.00")));
    }

    @Test
    void shouldTransferAndPersistBalanceChanges() {
        var response = transactionService.transfer(
                new TransferRequest(source.getId(), destination.getId(), new BigDecimal("200.00")));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.amount()).isEqualByComparingTo("200.00");
        assertThat(response.transactionId()).isNotNull();

        var updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        var updatedDestination = accountRepository.findById(destination.getId()).orElseThrow();

        assertThat(updatedSource.getBalance()).isEqualByComparingTo("800.00");
        assertThat(updatedDestination.getBalance()).isEqualByComparingTo("700.00");
        assertThat(transactionRepository.count()).isEqualTo(1);

        verify(notificationService).notify(
                source.getId(), "Alice", destination.getId(), "Bob", new BigDecimal("200.00"));
    }

    @Test
    void shouldPersistFailedTransactionOnInsufficientFunds() {
        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(source.getId(), destination.getId(), new BigDecimal("5000.00"))))
                .isInstanceOf(InsufficientFundsException.class);

        var updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo("1000.00");

        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.findAll().getFirst().getStatus()).isEqualTo(TransactionStatus.FAILED);

        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldThrowOnSameAccountTransferAndNotPersistTransaction() {
        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(source.getId(), source.getId(), new BigDecimal("100.00"))))
                .isInstanceOf(TransferToSameAccountException.class);

        assertThat(transactionRepository.count()).isZero();
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldThrowWhenSourceAccountNotFound() {
        var unknownId = UUID.randomUUID();
        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(unknownId, destination.getId(), new BigDecimal("100.00"))))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        assertThat(transactionRepository.count()).isZero();
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldReturnPaginatedStatement() {
        transactionService.transfer(new TransferRequest(source.getId(), destination.getId(), new BigDecimal("100.00")));
        transactionService.transfer(new TransferRequest(source.getId(), destination.getId(), new BigDecimal("50.00")));

        var page1 = transactionService.getTransactionByAccountId(source.getId(), 0, 1);
        var page2 = transactionService.getTransactionByAccountId(source.getId(), 1, 1);

        assertThat(page1.totalElements()).isEqualTo(2);
        assertThat(page1.content()).hasSize(1);
        assertThat(page2.content()).hasSize(1);
    }

    @Test
    void shouldFindTransactionById() {
        var created = transactionService.transfer(
                new TransferRequest(source.getId(), destination.getId(), new BigDecimal("100.00")));

        var found = transactionService.findById(created.transactionId());

        assertThat(found.transactionId()).isEqualTo(created.transactionId());
        assertThat(found.amount()).isEqualByComparingTo("100.00");
        assertThat(found.status()).isEqualTo("COMPLETED");
        assertThat(found.sourceAccountName()).isEqualTo("Alice");
        assertThat(found.destinationAccountName()).isEqualTo("Bob");
    }

    @Test
    void shouldThrowWhenTransactionNotFound() {
        var randomId = UUID.randomUUID();
        assertThatThrownBy(() -> transactionService.findById(randomId))
                .isInstanceOf(TransferNotFoundException.class)
                .hasMessageContaining(randomId.toString());
    }

    @Test
    void shouldHandleConcurrentTransfersWithoutDeadlock() throws InterruptedException {
        var threads = 20;
        var executor = Executors.newFixedThreadPool(threads);
        var latch = new CountDownLatch(threads);
        var unexpectedErrors = Collections.synchronizedList(new ArrayList<Exception>());

        var initialTotal = source.getBalance().add(destination.getBalance());

        for (int i = 0; i < threads / 2; i++) {
            executor.submit(() -> {
                try {
                    transactionService.transfer(
                            new TransferRequest(source.getId(), destination.getId(), new BigDecimal("10.00")));
                } catch (InsufficientFundsException ignored) {
                    // expected under contention
                } catch (Exception e) {
                    unexpectedErrors.add(e);
                } finally {
                    latch.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    transactionService.transfer(
                            new TransferRequest(destination.getId(), source.getId(), new BigDecimal("10.00")));
                } catch (InsufficientFundsException ignored) {
                    // expected under contention
                } catch (Exception e) {
                    unexpectedErrors.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(unexpectedErrors)
                .as("No deadlocks or unexpected errors should occur")
                .isEmpty();

        var updatedSource = accountRepository.findById(source.getId()).orElseThrow();
        var updatedDest = accountRepository.findById(destination.getId()).orElseThrow();
        assertThat(updatedSource.getBalance().add(updatedDest.getBalance()))
                .as("Total funds must be conserved across all concurrent transfers")
                .isEqualByComparingTo(initialTotal);
    }
}
