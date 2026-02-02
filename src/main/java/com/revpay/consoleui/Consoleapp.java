package com.revpay.consoleui;

import com.revpay.entity.Notification;
import com.revpay.entity.User;
import com.revpay.entity.enums.NotificationType;
import com.revpay.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

@Component
public class Consoleapp {
    private static final Logger log = LoggerFactory.getLogger(Consoleapp.class);
    private final Scanner scanner = new Scanner(System.in);

    @Autowired
    private NotificationService notificationService;

    public void displayMessage(String message) {
        log.info(message);
        System.out.println(message);
    }

    public void displayError(String message) {
        log.error(message);
        System.err.println("ERROR: " + message);
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                displayError("Invalid number, please try again.");
            }
        }
    }

    public long readLong(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Long.parseLong(scanner.nextLine());
            } catch (NumberFormatException e) {
                displayError("Invalid number, please try again.");
            }
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException e) {
                displayError("Invalid amount, please try again.");
            }
        }
    }

    // Notification menu and related methods — note no "consoleapp." prefix, just direct calls

    public void notificationMenu(User user) {
        while (true) {
            int choice = readInt(
                    "\n--- NOTIFICATIONS ---\n" +
                            "1. View All Notifications\n" +
                            "2. View Unread Notifications\n" +
                            "3. Mark Notification as Read\n" +
                            "4. Mark Notification as Unread\n" +
                            "5. View Notification Preferences\n" +
                            "6. Update Notification Preferences\n" +
                            "0. Back\n" +
                            "Choice: ");

            switch (choice) {
                case 1:
                    displayNotifications(user, false);
                    break;
                case 2:
                    displayNotifications(user, true);
                    break;
                case 3:
                    markNotificationReadUnread(user, true);
                    break;
                case 4:
                    markNotificationReadUnread(user, false);
                    break;
                case 5:
                    showNotificationPreferences(user);
                    break;
                case 6:
                    updateNotificationPreferences(user);
                    break;
                case 0:
                    return; // back to previous menu
                default:
                    displayError("Invalid choice, please try again.");
            }
        }
    }

    private void displayNotifications(User user, boolean onlyUnread) {
        List<Notification> notifications = notificationService.getUserNotifications(user, onlyUnread);
        if (notifications.isEmpty()) {
            displayMessage("No notifications found.");
            return;
        }
        displayMessage("\n--- Notifications ---");
        for (Notification n : notifications) {
            displayMessage("ID: " + n.getId() +
                    " | Type: " + n.getType() +
                    " | Message: " + n.getMessage() +
                    " | Read: " + (n.isReadStatus() ? "Yes" : "No") +
                    " | Date: " + n.getCreatedAt());
        }
    }

    private void markNotificationReadUnread(User user, boolean markRead) {
        Long id = readLong("Enter Notification ID: ");
        try {
            if (markRead) {
                notificationService.markAsRead(id, user);
                displayMessage("Notification marked as read.");
            } else {
                notificationService.markAsUnread(id, user);
                displayMessage("Notification marked as unread.");
            }
        } catch (RuntimeException e) {
            displayError("Error: " + e.getMessage());
        }
    }

    private void showNotificationPreferences(User user) {
        // Assuming you added methods for preferences in NotificationService
        Set<NotificationType> prefs = notificationService.getUserNotificationPreferences(user);
        displayMessage("\n--- Your Notification Preferences ---");
        for (NotificationType type : NotificationType.values()) {
            displayMessage(type + ": " + (prefs.contains(type) ? "Enabled" : "Disabled"));
        }
    }

    private void updateNotificationPreferences(User user) {
        Set<NotificationType> currentPrefs = notificationService.getUserNotificationPreferences(user);
        displayMessage("\n--- Update Notification Preferences ---");
        displayMessage("Type 'yes' to enable, anything else to disable.");

        Set<NotificationType> newPrefs = new HashSet<>();
        for (NotificationType type : NotificationType.values()) {
            String input = readLine(type + " (currently " +
                    (currentPrefs.contains(type) ? "Enabled" : "Disabled") + "): ");
            if ("yes".equalsIgnoreCase(input.trim())) {
                newPrefs.add(type);
            }
        }

        notificationService.updateUserNotificationPreferences(user, newPrefs);
        displayMessage("Notification preferences updated.");
    }
}
