package com.renner.bank.mapper;

import com.renner.bank.domain.Account;
import com.renner.bank.domain.Transaction;
import com.renner.bank.domain.TransactionStatus;
import com.renner.bank.dto.TransactionResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = new TransactionMapper();

    @Test
    void shouldCreateTransaction() {
        Account source = new Account("Alice", new BigDecimal("1000.00"));
        Account destination = new Account("Bob", new BigDecimal("500.00"));

        Transaction transaction = transactionMapper.createTransaction(
                source,
                destination,
                new BigDecimal("100.00"),
                TransactionStatus.COMPLETED
        );

        assertThat(transaction.getSource()).isSameAs(source);
        assertThat(transaction.getDestination()).isSameAs(destination);
        assertThat(transaction.getAmount()).isEqualByComparingTo("100.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(transaction.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldMapTransactionToResponse() {
        UUID sourceId = UUID.randomUUID();
        UUID destinationId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        Account source = new Account("Alice", new BigDecimal("1000.00"));
        source.setId(sourceId);
        Account destination = new Account("Bob", new BigDecimal("500.00"));
        destination.setId(destinationId);

        Transaction transaction = new Transaction(
                source,
                destination,
                new BigDecimal("100.00"),
                TransactionStatus.COMPLETED,
                createdAt
        );
        java.lang.reflect.Field idField;
        try {
            idField = Transaction.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(transaction, transactionId);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }

        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertThat(response.transactionId()).isEqualTo(transactionId);
        assertThat(response.sourceAccountId()).isEqualTo(sourceId);
        assertThat(response.sourceAccountName()).isEqualTo("Alice");
        assertThat(response.destinationAccountId()).isEqualTo(destinationId);
        assertThat(response.destinationAccountName()).isEqualTo("Bob");
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
