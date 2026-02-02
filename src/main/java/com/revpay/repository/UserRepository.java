package com.revpay.repository;

import com.revpay.entity.User;
import com.revpay.entity.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmailOrPhone(String email, String phone);

    List<User> findByAccountType(AccountType accountType);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
