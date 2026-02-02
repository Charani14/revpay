package com.revpay.repository;

import com.revpay.entity.PaymentMethod;
import com.revpay.entity.User;
import com.revpay.entity.enums.PaymentMethodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUser(User user);

    List<PaymentMethod> findByUserAndPaymentMethodType(User user, PaymentMethodType type);

    Optional<PaymentMethod> findByUserAndIsDefaultTrue(User user);

    Optional<PaymentMethod> findByIdAndUser(Long id, User user);

}
