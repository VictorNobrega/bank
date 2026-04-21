package com.renner.bank.mapper;

import com.renner.bank.domain.Account;
import com.renner.bank.domain.Transaction;
import com.renner.bank.domain.TransactionStatus;
import com.renner.bank.dto.TransactionResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class TransactionMapper {

    public Transaction createTransaction(Account source,
                                         Account destination,
                                         BigDecimal amount,
                                         TransactionStatus status) {
        return new Transaction(
                source,
                destination,
                amount,
                status,
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }

    public TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSource().getId(),
                transaction.getSource().getName(),
                transaction.getDestination().getId(),
                transaction.getDestination().getName(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
