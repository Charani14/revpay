package com.revpay.service;


import com.revpay.entity.User;
import com.revpay.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User login(String identifier, String password) {

        User user = userRepository
                .findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPasswordHash().equals(password)) {
            throw new RuntimeException("Invalid password");
        }


        return user;
    }
    public String hashPin(String pin) {
        // Use secure hash, e.g. BCrypt or SHA-256 with salt, example:
        return BCrypt.hashpw(pin, BCrypt.gensalt());
    }

    public boolean verifyPin(User user, String pin) {
        // Compare entered PIN hash with stored user.getTransactionPinHash()
        return BCrypt.checkpw(pin, user.getTransactionPinHash());
    }

}
