package com.revpay.repository;

import com.revpay.entity.Notification;
import com.revpay.entity.User;
import com.revpay.entity.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    List<Notification> findByUserAndReadStatusFalseOrderByCreatedAtDesc(User user);

    Optional<Notification> findByUserAndType(User user, NotificationType type);
}
