package com.revpay.service;


import com.revpay.entity.SecurityQuestion;
import com.revpay.entity.User;
import com.revpay.exception.InvalidCredentialsException;
import com.revpay.exception.UserNotFoundException;
import com.revpay.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SecurityService {

    private final UserRepository userRepository;

    public SecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Value("${revpay.login.max-attempts}")
    private int maxFailedAttempts;

    @Value("${revpay.session.timeout.minutes}")
    private long lockTimeDuration;


    @Autowired
    private PasswordEncoder passwordEncoder;


    public User login(String identifier, String rawPassword) {
        User user = userRepository
                .findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isAccountLocked()) {
            throw new InvalidCredentialsException("Account is locked due to multiple failed login attempts.");
        }

        if (!user.getPasswordHash().equals(rawPassword)) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            if (failedAttempts >= maxFailedAttempts) {
                user.setAccountLocked(true);
            }

            userRepository.save(user);

            throw new InvalidCredentialsException(failedAttempts >= maxFailedAttempts
                    ? "Account locked due to " + maxFailedAttempts + " failed login attempts."
                    : "Invalid password. Attempts left: " + (maxFailedAttempts - failedAttempts));
        }

        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
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
    private final SecureRandom random = new SecureRandom();
    private final Map<Long, String> user2FACodes = new ConcurrentHashMap<>(); // userId to code

    // Generate and store a 6-digit code for user
    public String generate2FACode(User user) {
        int code = 100000 + random.nextInt(900000); // 6-digit code
        String codeStr = String.valueOf(code);
        user2FACodes.put(user.getId(), codeStr);
        return codeStr;
    }

    // Verify entered code
    public boolean verify2FACode(User user, String inputCode) {
        String correctCode = user2FACodes.get(user.getId());
        if (correctCode != null && correctCode.equals(inputCode)) {
            user2FACodes.remove(user.getId()); // remove after successful verification
            return true;
        }
        return false;
    }

    public User findUserByEmailOrPhone(String identifier) {
        return userRepository.findByEmailOrPhone(identifier, identifier).orElse(null);
    }

    public boolean verifySecurityAnswer(User user, SecurityQuestion question, String answer) {
        // Assuming stored answers are hashed for security
        // You can use BCrypt or simple equals if plain text (not recommended)
        // Example if stored hashed:
        return BCrypt.checkpw(answer.trim().toLowerCase(), question.getAnswerHash());
    }

    public void resetPassword(User user, String newRawPassword) {
        String hashed = passwordEncoder.encode(newRawPassword);
        user.setPasswordHash(hashed);
        userRepository.save(user);
    }

}
