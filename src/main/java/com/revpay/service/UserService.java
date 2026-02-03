package com.revpay.service;

import com.revpay.entity.User;
import com.revpay.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {
        user.setWalletBalance(0.0);
        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


        public void uploadDocument(User user, String documentPath) {
            String existingDocs = user.getVerificationDocuments();

            if (existingDocs == null || existingDocs.isEmpty()) {
                user.setVerificationDocuments(documentPath);
            } else {
                user.setVerificationDocuments(existingDocs + "," + documentPath);
            }
        }

        // ✅ SIMULATED VERIFICATION
        public boolean verifyDocuments(User user) {

            List<String> docs = getDocuments(user);

            // Simple realistic rule:
            // At least 2 documents required
            if (docs.size() < 2) {
                return false;
            }

            // You can add more rules here later
            user.setBusinessVerified(true);
            return true;
        }


    public List<String> getDocuments(User user) {
        if (user.getVerificationDocuments() == null || user.getVerificationDocuments().trim().isEmpty()) {
            return Collections.emptyList();   // ✅ Java 8 compatible
        }

        return Arrays.asList(user.getVerificationDocuments().split(",")); // ✅ Java 8 compatible
    }
}
