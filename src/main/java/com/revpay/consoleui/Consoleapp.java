package com.revpay.consoleui;

import com.revpay.entity.Invoice;
import com.revpay.entity.Notification;
import com.revpay.entity.SecurityQuestion;
import com.revpay.entity.User;
import com.revpay.entity.enums.NotificationType;
import com.revpay.service.InvoiceService;
import com.revpay.service.NotificationService;
import com.revpay.service.SecurityService;
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
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private SecurityService securityService;

    public void displayMessage(String message) {
        log.info(message);
    }

    public void displayError(String message) {
        log.error(message);
    }

    public String readLine(String prompt) {
        log.info(prompt);
        return scanner.nextLine();
    }

    public int readInt(String prompt) {
        while (true) {
            try {
                log.info(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                displayError("Invalid number, please try again.");
            }
        }
    }

    public long readLong(String prompt) {
        while (true) {
            try {
                log.info(prompt);
                return Long.parseLong(scanner.nextLine());
            } catch (NumberFormatException e) {
                displayError("Invalid number, please try again.");
            }
        }
    }

    public double readDouble(String prompt) {
        while (true) {
            try {
                log.info(prompt);
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

    public void invoiceMenu(User user) {
        while (true) {
            int choice = readInt(
                    "\n--- INVOICE MENU ---\n" +
                            "1. Create Invoice\n" +
                            "2. View All Invoices\n" +
                            "3. Pay Invoice\n" +
                            "4. Cancel Invoice\n" +
                            "0. Back to Business Menu\n" +
                            "Choice: ");

            switch (choice) {
                case 1:
                    try {
                        String customerInfo = readLine("Customer Info (Name/Contact): ");
                        String itemizedDetails = readLine("Itemized Details (items, quantities, prices): ");
                        String paymentTerms = readLine("Payment Terms (e.g., Net 30): ");
                        double totalAmount = readDouble("Total Amount: ");

                        Invoice invoice = invoiceService.createInvoice(
                                user, customerInfo, itemizedDetails, paymentTerms, totalAmount
                        );

                        notificationService.notifyUser(user, "Invoice created successfully, ID: " + invoice.getId());
                        displayMessage("Invoice created successfully with ID: " + invoice.getId());
                    } catch (Exception e) {
                        log.error("Failed to create invoice", e);
                        displayError("Failed to create invoice. Please try again.");
                    }
                    break;

                case 2:
                    List<Invoice> invoices = invoiceService.getInvoicesForUser(user);
                    if (invoices.isEmpty()) {
                        displayMessage("No invoices found.");
                    } else {
                        displayMessage("\n--- Your Invoices ---");
                        for (Invoice inv : invoices) {
                            displayMessage("ID: " + inv.getId() +
                                    " | Customer: " + inv.getCustomerInfo() +
                                    " | Amount: ₹" + inv.getTotalAmount() +
                                    " | Status: " + inv.getStatus() +
                                    " | Created At: " + inv.getCreatedAt());
                        }
                    }
                    break;

                case 3:
                    try {
                        long invoiceId = Long.parseLong(readLine("Enter Invoice ID to pay: "));
                        boolean success = invoiceService.payInvoice(invoiceId, user);
                        if (success) {
                            notificationService.notifyUser(user, "Invoice ID " + invoiceId + " paid successfully.");
                            displayMessage("Invoice paid successfully.");
                        } else {
                            displayError("Payment failed. Invoice might not exist or is already paid.");
                        }
                    } catch (NumberFormatException e) {
                        displayError("Invalid invoice ID format.");
                    } catch (Exception e) {
                        log.error("Failed to pay invoice", e);
                        displayError("Failed to pay invoice. Please try again.");
                    }
                    break;

                case 4:
                    try {
                        long invoiceId = Long.parseLong(readLine("Enter Invoice ID to cancel: "));
                        boolean success = invoiceService.cancelInvoice(invoiceId, user);
                        if (success) {
                            notificationService.notifyUser(user, "Invoice ID " + invoiceId + " canceled successfully.");
                            displayMessage("Invoice canceled successfully.");
                        } else {
                            displayError("Cancellation failed. Invoice might not exist or is already paid.");
                        }
                    } catch (NumberFormatException e) {
                        displayError("Invalid invoice ID format.");
                    } catch (Exception e) {
                        log.error("Failed to cancel invoice", e);
                        displayError("Failed to cancel invoice. Please try again.");
                    }
                    break;

                case 0:
                    return;

                default:
                    displayError("Invalid choice, please try again.");
            }
        }
    }
    public void passwordRecoveryMenu(String identifier) {
        try {
            User user = securityService.findUserByEmailOrPhone(identifier);
            if (user == null) {
                displayError("User not found.");
                return;
            }

            List<SecurityQuestion> questions = user.getSecurityQuestions();
            if (questions == null || questions.isEmpty()) {
                displayError("No security questions found for this user.");
                return;
            }

            displayMessage("Answer the following security questions:");

            int correctAnswers = 0;

            for (SecurityQuestion question : questions) {
                String answer = readLine(question.getQuestion() + ": ");
                if (securityService.verifySecurityAnswer(user, question, answer)) {
                    correctAnswers++;
                }
            }

            if (correctAnswers == questions.size()) {
                String newPassword = readLine("Enter your new password: ");
                String confirmPassword = readLine("Confirm your new password: ");

                if (!newPassword.equals(confirmPassword)) {
                    displayError("Passwords do not match. Aborting password reset.");
                    return;
                }

                securityService.resetPassword(user, newPassword);
                displayMessage("Password reset successful. You can now log in with your new password.");
            } else {
                displayError("Security answers incorrect. Cannot reset password.");
            }

        } catch (Exception e) {
            displayError("An error occurred during password recovery.");
            log.error("Password recovery error", e);
        }
    }

}
