package com.renner.bank.repository;

import com.renner.bank.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.source
            JOIN FETCH t.destination
            WHERE t.source.id = :accountId OR t.destination.id = :accountId
            ORDER BY t.createdAt DESC
            """,
            countQuery = """
                    SELECT COUNT(t) FROM Transaction t
                    WHERE t.source.id = :accountId OR t.destination.id = :accountId
                    """)
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);

    @Query(value = """
            SELECT t FROM Transaction t
            JOIN FETCH t.source
            JOIN FETCH t.destination
            ORDER BY t.createdAt DESC
            """,
            countQuery = "SELECT COUNT(t) FROM Transaction t")
    Page<Transaction> findAllWithAccounts(Pageable pageable);
}