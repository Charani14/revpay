package com.revpay.repository;

import com.revpay.entity.Invoice;
import com.revpay.entity.User;
import com.revpay.entity.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByBusinessUser(User businessUser);

    List<Invoice> findByBusinessUserAndStatus(User businessUser, InvoiceStatus status);
}
