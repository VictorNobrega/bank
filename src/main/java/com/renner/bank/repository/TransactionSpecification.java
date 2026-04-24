package com.renner.bank.repository;

import com.renner.bank.domain.Transaction;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class TransactionSpecification {

    private TransactionSpecification() {}

    public static Specification<Transaction> byAccountId(UUID accountId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("source").get("id"), accountId),
                cb.equal(root.get("destination").get("id"), accountId)
        );
    }

    public static Specification<Transaction> withEagerAccounts() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("source", JoinType.INNER);
                root.fetch("destination", JoinType.INNER);
            }
            return cb.conjunction();
        };
    }
}
