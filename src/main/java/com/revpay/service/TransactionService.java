package com.revpay.service;

import com.revpay.entity.Transaction;
import com.revpay.entity.User;
import com.revpay.entity.enums.TransactionStatus;
import com.revpay.entity.enums.TransactionType;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /* ---------------------------------------------------
       SEND MONEY
    --------------------------------------------------- */
    public Transaction sendMoney(User sender, String receiverIdentifier, double amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        if (sender.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        User receiver = findUserByEmailOrPhone(receiverIdentifier)
                .orElseThrow(() ->
                        new RuntimeException("Receiver not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new RuntimeException("Cannot send money to yourself");
        }

        // Wallet update
        sender.setWalletBalance(sender.getWalletBalance() - amount);
        receiver.setWalletBalance(receiver.getWalletBalance() + amount);

        userRepository.save(sender);
        userRepository.save(receiver);

        Transaction tx = new Transaction();
        tx.setSender(sender);
        tx.setReceiver(receiver);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.SEND);
        tx.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(tx);
    }

    /* ---------------------------------------------------
       REQUEST MONEY
    --------------------------------------------------- */
    public Transaction requestMoney(User requester,
                                    String payerIdentifier,
                                    double amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        User payer = findUserByEmailOrPhone(payerIdentifier)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (requester.getId().equals(payer.getId())) {
            throw new RuntimeException("Cannot request money from yourself");
        }

        Transaction requestTx = new Transaction();
        requestTx.setSender(requester);   // requester
        requestTx.setReceiver(payer);     // payer
        requestTx.setAmount(amount);
        requestTx.setTransactionType(TransactionType.REQUEST);
        requestTx.setStatus(TransactionStatus.PENDING);

        return transactionRepository.save(requestTx);
    }

    /* ---------------------------------------------------
       ACCEPT REQUEST
    --------------------------------------------------- */
    public Transaction acceptRequest(Long requestId, User payer) {

        Transaction requestTx = transactionRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Request not found"));

        if (!requestTx.getReceiver().getId().equals(payer.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        if (requestTx.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        double amount = requestTx.getAmount();

        if (payer.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        User requester = requestTx.getSender();

        payer.setWalletBalance(payer.getWalletBalance() - amount);
        requester.setWalletBalance(requester.getWalletBalance() + amount);

        userRepository.save(payer);
        userRepository.save(requester);

        requestTx.setStatus(TransactionStatus.COMPLETED);
        requestTx.setTransactionType(TransactionType.SEND);

        return transactionRepository.save(requestTx);
    }

    /* ---------------------------------------------------
       DECLINE REQUEST
    --------------------------------------------------- */
    public Transaction declineRequest(Long requestId, User payer) {

        Transaction requestTx = transactionRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Request not found"));

        if (!requestTx.getReceiver().getId().equals(payer.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        if (requestTx.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        requestTx.setStatus(TransactionStatus.DECLINED);
        return transactionRepository.save(requestTx);
    }

    /* ---------------------------------------------------
       PENDING REQUESTS
    --------------------------------------------------- */
    public List<Transaction> getPendingRequestsForUser(User user) {
        return transactionRepository
                .findByReceiverAndTransactionTypeAndStatus(
                        user,
                        TransactionType.REQUEST,
                        TransactionStatus.PENDING
                );
    }

    /* ---------------------------------------------------
       WITHDRAW MONEY
    --------------------------------------------------- */
    public Transaction withdrawMoney(User user, double amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid withdrawal amount");
        }

        if (user.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        user.setWalletBalance(user.getWalletBalance() - amount);
        userRepository.save(user);

        Transaction tx = new Transaction();
        tx.setSender(user);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.WITHDRAW);
        tx.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(tx);
    }

    /* ---------------------------------------------------
       TRANSACTION HISTORY
    --------------------------------------------------- */
    public List<Transaction> getTransactionHistory(
            User user,
            LocalDate fromDate,
            LocalDate toDate,
            TransactionType type,
            TransactionStatus status,
            String search
    ) {

        return transactionRepository.findBySenderOrReceiver(user, user)
                .stream()
                .filter(tx -> fromDate == null ||
                        !tx.getCreatedAt().toLocalDate().isBefore(fromDate))
                .filter(tx -> toDate == null ||
                        !tx.getCreatedAt().toLocalDate().isAfter(toDate))
                .filter(tx -> type == null ||
                        tx.getTransactionType() == type)
                .filter(tx -> status == null ||
                        tx.getStatus() == status)
                .filter(tx -> search == null ||
                        (tx.getNote() != null &&
                                tx.getNote().toLowerCase().contains(search.toLowerCase())))
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /* ---------------------------------------------------
       EXPORT HISTORY
    --------------------------------------------------- */
    public void exportTransactionHistory(
            List<Transaction> transactions,
            String filePath) {

        try (PrintWriter writer = new PrintWriter(new File(filePath))) {

            writer.println("ID,TYPE,STATUS,AMOUNT,SENDER,RECEIVER,DATE,NOTE");

            for (Transaction tx : transactions) {
                writer.println(
                        tx.getId() + "," +
                                tx.getTransactionType() + "," +
                                tx.getStatus() + "," +
                                tx.getAmount() + "," +
                                (tx.getSender() != null ? tx.getSender().getEmail() : "N/A") + "," +
                                (tx.getReceiver() != null ? tx.getReceiver().getEmail() : "N/A") + "," +
                                tx.getCreatedAt() + "," +
                                (tx.getNote() != null ? tx.getNote() : "")
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to export transaction history", e);
        }
    }

    /* ---------------------------------------------------
       COMMON USER FINDER
    --------------------------------------------------- */
    private Optional<User> findUserByEmailOrPhone(String identifier) {

        Optional<User> userByEmail = userRepository.findByEmail(identifier);
        if (userByEmail.isPresent()) {
            return userByEmail;
        }
        return userRepository.findByPhone(identifier);
    }
}
