package com.revpay.repository;

import com.revpay.entity.LoanApplication;
import com.revpay.entity.User;
import com.revpay.entity.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByBusinessUser(User businessUser);

    List<LoanApplication> findByBusinessUserAndStatus(User businessUser, LoanStatus status);
}
