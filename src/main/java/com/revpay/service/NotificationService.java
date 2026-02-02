package com.revpay.service;

import com.revpay.entity.Notification;
import com.revpay.entity.User;
import com.revpay.entity.enums.NotificationType;
import com.revpay.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    private static final String PREF_SEPARATOR = ",";

    // Fetch all or only unread notifications for a user, ordered newest first
    public List<Notification> getUserNotifications(User user, boolean onlyUnread) {
        if (onlyUnread) {
            return notificationRepository.findByUserAndReadStatusFalseOrderByCreatedAtDesc(user);
        }
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Mark a notification as read by id for the user
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().equals(user)) {
            throw new RuntimeException("Unauthorized access");
        }

        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    // Mark a notification as unread by id for the user
    public void markAsUnread(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().equals(user)) {
            throw new RuntimeException("Unauthorized access");
        }

        notification.setReadStatus(false);
        notificationRepository.save(notification);
    }

    // Send a notification to user (no preference check)


    public void sendNotification(User user, NotificationType type, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReadStatus(false);
        notificationRepository.save(notification);
    }
    /**
     * Helper method to notify user with default notification type (e.g., ALERT)
     */
    public void notifyUser(User user, String message) {
        sendNotification(user, NotificationType.ALERT, message);
    }


    // --- Preference management below ---

    /**
     * Fetch user notification preferences from special notification record.
     * If none found, default to enabling all types except PREFERENCE itself.
     */
    public Set<NotificationType> getUserNotificationPreferences(User user) {
        Notification prefNotification = notificationRepository
                .findByUserAndType(user, NotificationType.PREFERENCE)
                .orElse(null);

        if (prefNotification == null || prefNotification.getMessage() == null || prefNotification.getMessage().isEmpty()) {
            // No prefs saved - return all enabled by default except PREFERENCE
            return EnumSet.allOf(NotificationType.class).stream()
                    .filter(t -> t != NotificationType.PREFERENCE)
                    .collect(Collectors.toSet());
        }

        String[] enabledTypes = prefNotification.getMessage().split(PREF_SEPARATOR);
        return Arrays.stream(enabledTypes)
                .map(String::trim)
                .map(NotificationType::valueOf)
                .collect(Collectors.toSet());
    }

    /**
     * Update user notification preferences by saving a special notification record
     * with type PREFERENCE and CSV list of enabled notification types in message field.
     */
    public void updateUserNotificationPreferences(User user, Set<NotificationType> enabledTypes) {
        Notification prefNotification = notificationRepository
                .findByUserAndType(user, NotificationType.PREFERENCE)
                .orElse(null);

        if (prefNotification == null) {
            prefNotification = new Notification();
            prefNotification.setUser(user);
            prefNotification.setType(NotificationType.PREFERENCE);
        }

        String message = enabledTypes.stream()
                .filter(t -> t != NotificationType.PREFERENCE) // exclude PREFERENCE itself
                .map(Enum::name)
                .collect(Collectors.joining(PREF_SEPARATOR));

        prefNotification.setMessage(message);
        prefNotification.setReadStatus(true); // mark read to distinguish from real notifications
        notificationRepository.save(prefNotification);
    }
}
