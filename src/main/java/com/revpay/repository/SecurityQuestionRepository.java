package com.revpay.repository;

import com.revpay.entity.SecurityQuestion;
import com.revpay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {

    List<SecurityQuestion> findByUser(User user);
}
