package com.revpay.service;

import com.revpay.entity.User;
import com.revpay.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final UserRepository userRepository;

    public PaymentService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void addMoney(User user, double amount) {

        if (amount <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        user.setWalletBalance(user.getWalletBalance() + amount);
        userRepository.save(user);
    }
}
