package com.revpay.service;

import com.revpay.entity.Invoice;
import com.revpay.entity.User;
import com.revpay.entity.enums.InvoiceStatus;
import com.revpay.repository.InvoiceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class to handle Invoice operations.
 *
 * @since 1.0
 */
@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    /**
     * Creates a new invoice for a business user.
     *
     * @param businessUser    The business user creating the invoice
     * @param customerInfo    Customer details
     * @param itemizedDetails Details of items and pricing
     * @param paymentTerms    Payment terms (e.g., Net 30)
     * @param totalAmount     Total invoice amount
     * @return The saved Invoice entity
     * @since 1.0
     */
    public Invoice createInvoice(User businessUser, String customerInfo, String itemizedDetails,
                                 String paymentTerms, Double totalAmount) {

        Invoice invoice = new Invoice();
        invoice.setBusinessUser(businessUser);
        invoice.setCustomerInfo(customerInfo);
        invoice.setItemizedDetails(itemizedDetails);
        invoice.setPaymentTerms(paymentTerms);
        invoice.setTotalAmount(totalAmount);
        invoice.setStatus(InvoiceStatus.UNPAID);

        return invoiceRepository.save(invoice);
    }

    /**
     * Retrieves all invoices associated with a given business user.
     *
     * @param businessUser The business user whose invoices to retrieve
     * @return List of invoices
     * @since 1.0
     */
    public List<Invoice> getInvoicesForUser(User businessUser) {
        return invoiceRepository.findByBusinessUser(businessUser);
    }

    /**
     * Marks an unpaid invoice as PAID.
     *
     * @param invoiceId    The invoice ID to pay
     * @param businessUser The user paying the invoice
     * @return true if payment was successful, false otherwise
     * @since 1.0
     */
    @Transactional
    public boolean payInvoice(Long invoiceId, User businessUser) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);

        if (!optionalInvoice.isPresent()) {
            return false;
        }

        Invoice invoice = optionalInvoice.get();

        if (!invoice.getBusinessUser().getId().equals(businessUser.getId()) ||
                invoice.getStatus() != InvoiceStatus.UNPAID) {
            return false;
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);
        return true;
    }

    /**
     * Cancels an unpaid invoice by marking its status as CANCELLED.
     * This method avoids deleting invoices for auditing purposes.
     *
     * @param invoiceId    The invoice ID to cancel
     * @param businessUser The user requesting cancellation
     * @return true if cancellation was successful, false otherwise
     * @since 1.0
     */
    @Transactional
    public boolean cancelInvoice(Long invoiceId, User businessUser) {
        Optional<Invoice> optionalInvoice = invoiceRepository.findById(invoiceId);

        if (!optionalInvoice.isPresent()) {
            return false;
        }

        Invoice invoice = optionalInvoice.get();

        if (!invoice.getBusinessUser().getId().equals(businessUser.getId()) ||
                invoice.getStatus() != InvoiceStatus.UNPAID) {
            return false;
        }

        // Set status to CANCELLED instead of deleting to keep record
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);
        return true;
    }
}
