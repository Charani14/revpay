package com.revpay.service;

import com.revpay.entity.LoanApplication;
import com.revpay.entity.User;
import com.revpay.entity.enums.LoanStatus;
import com.revpay.repository.LoanApplicationRepository;
import com.revpay.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class LoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final UserRepository userRepository;  // Need to fetch User entity

    public LoanService(LoanApplicationRepository loanApplicationRepository,
                       UserRepository userRepository) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.userRepository = userRepository;
    }

    public LoanApplication applyLoan(Long userId, double amount) {
        // Fetch User entity by userId
        User businessUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        LoanApplication loanApplication = new LoanApplication();
        loanApplication.setBusinessUser(businessUser);  // Set User object here
        loanApplication.setLoanAmount(amount);          // Use loanAmount (your entity field)
        loanApplication.setStatus(LoanStatus.PENDING);

        return loanApplicationRepository.save(loanApplication);
    }
}
