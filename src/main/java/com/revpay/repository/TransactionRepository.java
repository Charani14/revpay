package com.revpay.repository;

import com.revpay.entity.Transaction;
import com.revpay.entity.User;
import com.revpay.entity.enums.TransactionStatus;
import com.revpay.entity.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // All transactions related to a user
    List<Transaction> findBySenderOrReceiver(User sender, User receiver);

    // Filter by type
    List<Transaction> findBySenderOrReceiverAndTransactionType(
            User sender, User receiver, TransactionType type
    );

    // Filter by status
    List<Transaction> findBySenderOrReceiverAndStatus(
            User sender, User receiver, TransactionStatus status
    );

    // Date range filter
    List<Transaction> findBySenderOrReceiverAndCreatedAtBetween(
            User sender, User receiver, LocalDateTime start, LocalDateTime end
    );

    // Pending requests
    List<Transaction> findByReceiverAndTransactionTypeAndStatus(
            User receiver, TransactionType type, TransactionStatus status
    );

}
