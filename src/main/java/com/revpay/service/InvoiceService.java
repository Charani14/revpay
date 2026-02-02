package com.revpay.service;

import com.revpay.entity.Invoice;
import com.revpay.entity.Transaction;
import com.revpay.entity.User;
import com.revpay.entity.enums.AccountType;
import com.revpay.entity.enums.InvoiceStatus;
import com.revpay.repository.InvoiceRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          TransactionRepository transactionRepository,
                          UserRepository userRepository) {
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create an invoice linked to a transaction.
     * This method fetches transaction, business user and fills mandatory invoice fields.
     */
    public Invoice createInvoice(Long transactionId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction with ID " + transactionId + " not found"));

        User businessUser = null;

        if (transaction.getSender() != null && transaction.getSender().getAccountType() == AccountType.BUSINESS) {
            businessUser = transaction.getSender();
        } else if (transaction.getReceiver() != null && transaction.getReceiver().getAccountType() == AccountType.BUSINESS) {
            businessUser = transaction.getReceiver();
        } else {
            throw new RuntimeException("No business user found in this transaction");
        }

        Invoice invoice = new Invoice();
        invoice.setBusinessUser(businessUser);

        // Setting default or placeholder values
        invoice.setCustomerInfo("Default Customer Info");
        invoice.setItemizedDetails("Default Itemized Details");
        invoice.setPaymentTerms("Net 30 days");
        invoice.setTotalAmount(transaction.getAmount());  // Use transaction amount as invoice total

        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setCreatedAt(java.time.LocalDateTime.now());

        return invoiceRepository.save(invoice);
    }

}
