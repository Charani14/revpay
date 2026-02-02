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

    /**
     * Send money to a user identified by email or phone.
     * @param sender the user sending money
     * @param receiverIdentifier email or phone of the receiver
     * @param amount amount to send
     * @return saved Transaction
     */
    public Transaction sendMoney(User sender, String receiverIdentifier, double amount) {
        if (sender.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        Optional<User> receiverOpt = findUserByEmailOrPhone(receiverIdentifier);
        if (!receiverOpt.isPresent()) {  // <-- change here for Java 8 compatibility
            throw new RuntimeException("Receiver user not found with email or phone: " + receiverIdentifier);
        }

        User receiver = receiverOpt.get();

        sender.setWalletBalance(sender.getWalletBalance() - amount);
        receiver.setWalletBalance(receiver.getWalletBalance() + amount);

        userRepository.save(sender);
        userRepository.save(receiver);

        Transaction tx = new Transaction();
        tx.setSender(sender);
        tx.setReceiver(receiver);
        tx.setAmount(amount);
        tx.setTransactionType(TransactionType.SEND);
        // Remove TransactionStatus if not used; else keep it here.
        tx.setStatus(TransactionStatus.COMPLETED);

        return transactionRepository.save(tx);
    }

    private Optional<User> findUserByEmailOrPhone(String identifier) {
        // First try email
        Optional<User> userOpt = userRepository.findByEmail(identifier);
        if (userOpt.isPresent()) {
            return userOpt;
        }
        // Then try phone
        return userRepository.findByPhone(identifier);
    }

    public Transaction requestMoney(User requester, String payerIdOrEmailOrPhone, double amount) {
        User payer = null;

        Optional<User> userByEmail = userRepository.findByEmail(payerIdOrEmailOrPhone);
        if (userByEmail.isPresent()) {
            payer = userByEmail.get();
        } else {
            Optional<User> userByPhone = userRepository.findByPhone(payerIdOrEmailOrPhone);
            if (userByPhone.isPresent()) {
                payer = userByPhone.get();
            }
        }

        if (payer == null) {
            throw new RuntimeException("User to request money from not found");
        }



        Transaction requestTx = new Transaction();
        requestTx.setSender(requester); // requester initiates the request
        requestTx.setReceiver(payer);   // payer is requested to pay
        requestTx.setAmount(amount);
        requestTx.setTransactionType(TransactionType.REQUEST);
        requestTx.setStatus(TransactionStatus.PENDING);

        return transactionRepository.save(requestTx);
    }

    public Transaction acceptRequest(Long requestId, User payer) {
        Transaction requestTx = transactionRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!requestTx.getReceiver().getId().equals(payer.getId())) {
            throw new RuntimeException("Unauthorized action");
        }
        if (requestTx.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }
        double amount = requestTx.getAmount();
        if (payer.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance to accept request");
        }

        User requester = requestTx.getSender();

        // Deduct from payer
        payer.setWalletBalance(payer.getWalletBalance() - amount);
        // Add to requester
        requester.setWalletBalance(requester.getWalletBalance() + amount);

        userRepository.save(payer);
        userRepository.save(requester);

        // Update transaction status and type to SEND
        requestTx.setStatus(TransactionStatus.COMPLETED);
        requestTx.setTransactionType(TransactionType.SEND);

        return transactionRepository.save(requestTx);
    }

    public Transaction declineRequest(Long requestId, User payer) {
        Transaction requestTx = transactionRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!requestTx.getReceiver().getId().equals(payer.getId())) {
            throw new RuntimeException("Unauthorized action");
        }
        if (requestTx.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Request already processed");
        }

        requestTx.setStatus(TransactionStatus.DECLINED);
        return transactionRepository.save(requestTx);
    }
    public List<Transaction> getPendingRequestsForUser(User user) {
        return transactionRepository.findByReceiverAndTransactionTypeAndStatus(
                user,
                TransactionType.REQUEST,
                TransactionStatus.PENDING
        );
    }

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

    public List<Transaction> getTransactionHistory(
            User user,
            LocalDate fromDate,
            LocalDate toDate,
            TransactionType type,
            TransactionStatus status,
            String search
    ) {
        return transactionRepository
                .findBySenderOrReceiver(user, user)
                .stream()
                .filter(tx -> fromDate == null ||
                        !tx.getCreatedAt().toLocalDate().isBefore(fromDate))
                .filter(tx -> toDate == null ||
                        !tx.getCreatedAt().toLocalDate().isAfter(toDate))
                .filter(tx -> type == null || tx.getTransactionType() == type)
                .filter(tx -> status == null || tx.getStatus() == status)
                .filter(tx -> search == null ||
                        (tx.getNote() != null &&
                                tx.getNote().toLowerCase().contains(search.toLowerCase())))
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    public void exportTransactionHistory(
            List<Transaction> transactions,
            String filePath
    ) {
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


}


