package com.revpay.config;

import com.revpay.entity.User;
import com.revpay.exception.InvalidCredentialsException;
import com.revpay.exception.UserNotFoundException;
import com.revpay.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class Security {

    private final UserRepository userRepository;

    public Security(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    private static final Logger log = LoggerFactory.getLogger(Security.class);

    public User login(String identifier, String password) {

        User user = userRepository.findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found for {}", identifier);
                    return new UserNotFoundException("User not found");
                });

        if (!user.getPasswordHash().equals(password)) {
            log.warn("Login failed: invalid password for user {}", identifier);
            throw new InvalidCredentialsException("Invalid password");
        }


        log.info("User logged in successfully: {}", user.getEmail());
        return user;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
