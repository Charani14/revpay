package com.revpay.consoleui;

import com.revpay.entity.Transaction;
import com.revpay.entity.User;
import com.revpay.entity.enums.AccountType;
import com.revpay.entity.enums.TransactionStatus;
import com.revpay.entity.enums.TransactionType;
import com.revpay.exception.*;
import com.revpay.service.*;
import com.revpay.consoleui.Consoleapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class Main {

    private final SecurityService securityService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final PaymentService paymentService;
    private final LoanService loanService;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;
    private final PaymentMethodService paymentMethodService;


    private final Consoleapp consoleapp;
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public Main(SecurityService securityService, UserService userService, TransactionService transactionService, PaymentService paymentService, LoanService loanService, InvoiceService invoiceService, NotificationService notificationService, PaymentMethodService paymentMethodService, Consoleapp consoleapp) {
        this.securityService = securityService;
        this.userService = userService;
        this.transactionService = transactionService;
        this.paymentService = paymentService;
        this.loanService = loanService;
        this.invoiceService = invoiceService;
        this.notificationService = notificationService;
        this.paymentMethodService = paymentMethodService;
        this.consoleapp = consoleapp;
    }

    public void runApp() {
        User loggedInUser = null;

        while (true) {
            try {
                consoleapp.displayMessage("\n===== REV PAY =====");
                int choice = consoleapp.readInt(
                        "1. Register\n" +
                                "2. Login\n" +
                                "0. Exit\n" +
                                "Choice: ");
                switch (choice) {
                    case 1:
                        register();
                        break;
                    case 2:
                        loggedInUser = login();
                        break;
                    case 0:
                        consoleapp.displayMessage("Thank you for using RevPay");
                        return; // exit
                    default:
                        consoleapp.displayError("Invalid choice, please try again.");
                }
            } catch (Exception e) {
                log.error("Unexpected error in main menu", e);
                consoleapp.displayError("An unexpected error occurred. Please try again.");
            }

            while (loggedInUser != null) {
                try {
                    if (loggedInUser.getAccountType() == AccountType.PERSONAL) {
                        personalMenu(loggedInUser);
                    } else {
                        businessMenu(loggedInUser);
                    }
                    loggedInUser = null; // logout after menu
                } catch (Exception e) {
                    log.error("Unexpected error in user menu", e);
                    consoleapp.displayError("An error occurred while processing your request. Please try again.");
                }
            }
        }
    }

    private void register() {
        try {
            User user = new User();

            user.setFullName(consoleapp.readLine("Full Name: "));
            user.setEmail(consoleapp.readLine("Email: "));
            user.setPhone(consoleapp.readLine("Phone: "));
            user.setPasswordHash(consoleapp.readLine("Password: "));
            while (true) {
                String pin = consoleapp.readLine("Set Transaction PIN (4 digits): ");
                if (pin.matches("\\d{4}")) {
                    user.setTransactionPinHash(securityService.hashPin(pin));
                    break;
                } else {
                    consoleapp.displayError("PIN must be exactly 4 digits.");
                }
            }

            AccountType accountType = null;
            while (true) {
                String accTypeInput = consoleapp.readLine("Account Type (PERSONAL/BUSINESS): ").toUpperCase();
                try {
                    accountType = AccountType.valueOf(accTypeInput);
                    break;
                } catch (IllegalArgumentException e) {
                    consoleapp.displayError("Invalid account type. Please enter PERSONAL or BUSINESS.");
                }
            }

            user.setAccountType(accountType);

            // If BUSINESS account, ask business-specific details
            if (accountType == AccountType.BUSINESS) {
                user.setBusinessName(consoleapp.readLine("Business Name: "));
                user.setBusinessType(consoleapp.readLine("Business Type: "));
                user.setTaxId(consoleapp.readLine("Tax ID: "));
                user.setAddress(consoleapp.readLine("Business Address: "));
                user.setVerificationDocuments(consoleapp.readLine("Verification Documents (description or path): "));
            }


            userService.register(user);
            consoleapp.displayMessage("Registration successful!");
            log.info("User registered: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Registration failed", e);
            consoleapp.displayError("Registration failed: " + e.getMessage());
        }
    }


    private User login() {
        try {
            String id = consoleapp.readLine("Email or Phone: ");
            String pwd = consoleapp.readLine("Password: ");
            User user = securityService.login(id, pwd);
            consoleapp.displayMessage("Login successful!");
            return user;
        } catch (UserNotFoundException | InvalidCredentialsException e) {
            consoleapp.displayError("Login failed: " + e.getMessage());
            log.warn("Login failed", e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            consoleapp.displayError("An unexpected error occurred during login. Please try again.");
            return null;
        }
    }

    private void personalMenu(User user) {
        while (true) {
            int choice = consoleapp.readInt(
                    "\n--- PERSONAL MENU ---\n" +
                            "1. Add Money\n" +
                            "2. Send Money\n" +
                            "3. Request Money\n" +
                            "4. Manage Money Requests\n" +  // <-- Added here
                            "5. View Balance\n" +
                            "6. Withdraw Money\n" +
                            "7. Manage Payment Methods\n" +
                            "8. Transaction History\n" +
                            "9. Notifications\n" +
                            "0. Logout\n" +
                            "Choice: ");

            switch (choice) {
                case 1:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        double amount = consoleapp.readDouble("Amount: ");
                        paymentService.addMoney(user, amount);
                        notificationService.notifyUser(user,"Money added");
                        consoleapp.displayMessage("Money added successfully.");
                    } catch (Exception e) {
                        log.error("Failed to add money", e);
                        consoleapp.displayError("Failed to add money. Please try again.");
                    }
                    break;

                case 2:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        String receiverIdOrEmailOrPhone = consoleapp.readLine("Receiver Email or Phone: ");
                        double amt = consoleapp.readDouble("Amount: ");
                        transactionService.sendMoney(user, receiverIdOrEmailOrPhone, amt);
                        notificationService.notifyUser(user,"Money sent");
                        consoleapp.displayMessage("Money sent successfully.");
                    } catch (Exception e) {
                        log.error("Failed to send money", e);
                        consoleapp.displayError("Failed to send money. Please try again.");
                    }
                    break;

                case 3:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        String payerIdOrEmailOrPhone = consoleapp.readLine("Payer Email or Phone: ");
                        double amt = consoleapp.readDouble("Amount to request: ");
                        transactionService.requestMoney(user, payerIdOrEmailOrPhone, amt);
                        notificationService.notifyUser(user,"Money request sent");
                        consoleapp.displayMessage("Money request sent successfully.");
                    } catch (Exception e) {
                        log.error("Failed to request money", e);
                        consoleapp.displayError("Failed to request money. Please try again.");
                    }
                    break;

                case 4:
                    if (!verifyTransactionPin(user)) break;  // PIN required to manage requests
                    manageMoneyRequests(user);
                    break;

                case 5:
                    try {
                        consoleapp.displayMessage("Balance: ₹" + user.getWalletBalance());
                    } catch (Exception e) {
                        log.error("Failed to get balance", e);
                        consoleapp.displayError("Failed to retrieve balance. Please try again.");
                    }
                    break;

                case 6: // ✅ Withdraw Money
                    if (!verifyTransactionPin(user)) break;
                    try {
                        double amount = consoleapp.readDouble("Withdraw Amount: ");
                        transactionService.withdrawMoney(user, amount);
                        notificationService.notifyUser(user,"Money withdrawn");
                        consoleapp.displayMessage("Money withdrawn successfully.");
                    } catch (Exception e) {
                        log.error("Withdraw failed", e);
                        consoleapp.displayError(e.getMessage());
                    }
                    break;
                case 7:
                    if (!verifyTransactionPin(user)) break;
                    managePaymentMethods(user);
                    break;
                case 8:
                    if (!verifyTransactionPin(user)) break;
                    transactionHistoryMenu(user);
                    break;
                case 9:
                    consoleapp.notificationMenu(user);
                    break;

                case 0:
                    consoleapp.displayMessage("Logged out");
                    return;

                default:
                    consoleapp.displayError("Invalid choice, please try again.");
            }
        }
    }

    private void businessMenu(User user) {
        while (true) {
            int choice = consoleapp.readInt(
                    "\n--- BUSINESS MENU ---\n" +
                            "1. Add Money\n" +
                            "2. Send Money\n" +
                            "3. Request Money\n" +
                            "4. Manage Money Requests\n" +  // <-- Added here
                            "5. Apply Loan\n" +
                            "6. Create Invoice\n" +
                            "7. Withdraw Money\n" +
                            "8. Manage Payment Methods\n" +
                            "9. Transaction History\n" +
                            "10. Notifications\n" +
                            "0. Logout\n" +
                            "Choice: ");

            switch (choice) {
                case 1:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        double amount = consoleapp.readDouble("Amount: ");
                        paymentService.addMoney(user, amount);
                        notificationService.notifyUser(user,"Money added");
                        consoleapp.displayMessage("Money added successfully.");
                    } catch (Exception e) {
                        log.error("Failed to add money", e);
                        consoleapp.displayError("Failed to add money. Please try again.");
                    }
                    break;

                case 2:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        String receiverIdOrEmailOrPhone = consoleapp.readLine("Receiver Email or Phone: ");
                        double amt = consoleapp.readDouble("Amount: ");
                        transactionService.sendMoney(user, receiverIdOrEmailOrPhone, amt);
                        notificationService.notifyUser(user,"Money sent");
                        consoleapp.displayMessage("Money sent successfully.");
                    } catch (Exception e) {
                        log.error("Failed to send money", e);
                        consoleapp.displayError("Failed to send money. Please try again.");
                    }
                    break;

                case 3:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        String payerIdOrEmailOrPhone = consoleapp.readLine("Payer Email or Phone: ");
                        double amt = consoleapp.readDouble("Amount to request: ");
                        transactionService.requestMoney(user, payerIdOrEmailOrPhone, amt);
                        notificationService.notifyUser(user,"Money request sent");
                        consoleapp.displayMessage("Money request sent successfully.");
                    } catch (Exception e) {
                        log.error("Failed to request money", e);
                        consoleapp.displayError("Failed to request money. Please try again.");
                    }
                    break;

                case 4:
                    if (!verifyTransactionPin(user)) break;
                    manageMoneyRequests(user);
                    break;

                case 5:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        double loanAmount = consoleapp.readDouble("Loan Amount: ");
                        loanService.applyLoan(user.getId(), loanAmount);
                        notificationService.notifyUser(user,"Loan application submitted");
                        consoleapp.displayMessage("Loan application submitted successfully.");
                    } catch (Exception e) {
                        log.error("Failed to apply loan", e);
                        consoleapp.displayError("Failed to apply for loan. Please try again.");
                    }
                    break;

                case 6:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        Long txId = Long.parseLong(consoleapp.readLine("Transaction ID: "));
                        invoiceService.createInvoice(txId);
                        notificationService.notifyUser(user,"Invoice created");
                        consoleapp.displayMessage("Invoice created successfully.");
                    } catch (Exception e) {
                        log.error("Failed to create invoice", e);
                        consoleapp.displayError("Failed to create invoice. Please try again.");
                    }
                    break;
                case 7: // ✅ Withdraw Money
                    if (!verifyTransactionPin(user)) break;
                    try {
                        double amount = consoleapp.readDouble("Withdraw Amount: ");
                        transactionService.withdrawMoney(user, amount);
                        notificationService.notifyUser(user,"Money withdrawn");
                        consoleapp.displayMessage("Money withdrawn successfully.");
                    } catch (Exception e) {
                        log.error("Withdraw failed", e);
                        consoleapp.displayError(e.getMessage());
                    }
                    break;
                case 8:
                    if (!verifyTransactionPin(user)) break;
                    managePaymentMethods(user);
                    break;
                case 9:
                    if (!verifyTransactionPin(user)) break;
                    transactionHistoryMenu(user);
                    break;
                case 10:
                    consoleapp.notificationMenu(user);
                    break;

                case 0:
                    consoleapp.displayMessage("Logged out");
                    return;

                default:
                    consoleapp.displayError("Invalid choice, please try again.");
            }
        }
    }

    private boolean verifyTransactionPin(User user) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String inputPin = consoleapp.readLine("Enter your 4-digit transaction PIN: ");
            try {
                if (securityService.verifyPin(user, inputPin)) {
                    return true;
                } else {
                    consoleapp.displayError("Invalid PIN. Please try again.");
                }
            } catch (InvalidPinException e) {
                consoleapp.displayError("Invalid PIN format. Please enter exactly 4 digits.");
            } catch (Exception e) {
                log.error("Error verifying transaction PIN", e);
                consoleapp.displayError("Error verifying PIN. Please try again.");
            }
        }
        consoleapp.displayError("Failed PIN verification. Operation cancelled.");
        return false;
    }

    private void manageMoneyRequests(User user) {
        while (true) {
            List<Transaction> pendingRequests = transactionService.getPendingRequestsForUser(user);

            if (pendingRequests.isEmpty()) {
                consoleapp.displayMessage("No pending money requests.");
                return;
            }

            consoleapp.displayMessage("\n--- Pending Money Requests ---");
            for (Transaction tx : pendingRequests) {
                consoleapp.displayMessage("Request ID: " + tx.getId() +
                        ", From: " + tx.getSender().getEmail() +
                        ", Amount: ₹" + tx.getAmount());
            }

            int choice = consoleapp.readInt(
                    "Enter Request ID to respond or 0 to go back: ");
            if (choice == 0) {
                return;  // Exit request management
            }

            Transaction selectedRequest = pendingRequests.stream()
                    .filter(tx -> tx.getId().equals((long) choice))
                    .findFirst()
                    .orElse(null);

            if (selectedRequest == null) {
                consoleapp.displayError("Invalid Request ID, please try again.");
                continue;
            }

            int action = consoleapp.readInt(
                    "1. Accept Request\n" +
                            "2. Decline Request\n" +
                            "0. Back\n" +
                            "Choice: ");

            switch (action) {
                case 1:
                    if (!verifyTransactionPin(user)) break;
                    try {
                        transactionService.acceptRequest(selectedRequest.getId(), user);
                        notificationService.notifyUser(user,"Money request accepted");
                        consoleapp.displayMessage("Request accepted successfully.");
                    } catch (Exception e) {
                        log.error("Failed to accept request", e);
                        consoleapp.displayError("Failed to accept request: " + e.getMessage());
                    }
                    break;

                case 2:
                    try {
                        transactionService.declineRequest(selectedRequest.getId(), user);
                        notificationService.notifyUser(user,"Money request declined");
                        consoleapp.displayMessage("Request declined successfully.");
                    } catch (Exception e) {
                        log.error("Failed to decline request", e);
                        consoleapp.displayError("Failed to decline request: " + e.getMessage());
                    }
                    break;

                case 0:
                    break;

                default:
                    consoleapp.displayError("Invalid choice, please try again.");
            }
        }
    }

    private void managePaymentMethods(User user) {
        while (true) {
            int choice = consoleapp.readInt(
                    "\n--- PAYMENT METHODS ---\n" +
                            "1. Add Card\n" +
                            "2. Add Bank Account\n" +
                            "3. View Payment Methods\n" +
                            "4. Set Default Payment Method\n" +
                            "5. Remove Payment Method\n" +
                            "0. Back\n" +
                            "Choice: ");

            switch (choice) {

                case 1: // Add Card
                    if (!verifyTransactionPin(user)) break;
                    String cardNumber = consoleapp.readLine("Card Number: ");
                    String cardType = consoleapp.readLine("Card Type (VISA/MASTER): ");
                    String expiry = consoleapp.readLine("Expiry (MM/YY): ");
                    boolean makeDefaultCard =
                            consoleapp.readInt("Make default? (1=Yes, 0=No): ") == 1;

                    paymentMethodService.addCard(
                            user, cardNumber, cardType, expiry, makeDefaultCard
                    );
                    consoleapp.displayMessage("Card added successfully.");
                    break;

                case 2: // Add Bank
                    if (!verifyTransactionPin(user)) break;
                    String acc = consoleapp.readLine("Account Number: ");
                    String bank = consoleapp.readLine("Bank Name: ");
                    boolean makeDefaultBank =
                            consoleapp.readInt("Make default? (1=Yes, 0=No): ") == 1;

                    paymentMethodService.addBankAccount(
                            user, acc, bank, makeDefaultBank
                    );
                    consoleapp.displayMessage("Bank account added successfully.");
                    break;

                case 3: // View
                    paymentMethodService.getPaymentMethods(user)
                            .forEach(pm ->
                                    consoleapp.displayMessage(
                                            "ID: " + pm.getId() +
                                                    " | Type: " + pm.getPaymentMethodType() +
                                                    " | Default: " + pm.isDefault()
                                    )
                            );
                    break;

                case 4: // Set Default
                    if (!verifyTransactionPin(user)) break;
                    Long defId = Long.parseLong(
                            consoleapp.readLine("Payment Method ID: ")
                    );
                    paymentMethodService.setDefaultPaymentMethod(user, defId);
                    consoleapp.displayMessage("Default payment method updated.");
                    break;

                case 5: // Remove
                    if (!verifyTransactionPin(user)) break;
                    Long delId = Long.parseLong(
                            consoleapp.readLine("Payment Method ID: ")
                    );
                    paymentMethodService.removePaymentMethod(user, delId);
                    consoleapp.displayMessage("Payment method removed.");
                    break;

                case 0:
                    return;

                default:
                    consoleapp.displayError("Invalid choice.");
            }
        }
    }

    private void transactionHistoryMenu(User user) {

        LocalDate fromDate = null;
        LocalDate toDate = null;
        TransactionType type = null;
        TransactionStatus status = null;
        String search = null;

        while (true) {
            int choice = consoleapp.readInt(
                    "\n--- TRANSACTION HISTORY ---\n" +
                            "1. Apply Filters\n" +
                            "2. Search by Note\n" +
                            "3. View Transactions\n" +
                            "4. Export to CSV\n" +
                            "0. Back\n" +
                            "Choice: ");

            switch (choice) {

                case 1:
                    String from = consoleapp.readLine("From date (yyyy-mm-dd / blank): ");
                    String to = consoleapp.readLine("To date (yyyy-mm-dd / blank): ");
                    String t = consoleapp.readLine("Type (SEND/RECEIVE/WITHDRAW/DEPOSIT / blank): ");
                    String s = consoleapp.readLine("Status (SUCCESS/PENDING/FAILED / blank): ");

                    fromDate = (from == null || from.trim().isEmpty()) ? null : LocalDate.parse(from);
                    toDate = (to == null || to.trim().isEmpty()) ? null : LocalDate.parse(to);
                    type = (t == null || t.trim().isEmpty()) ? null : TransactionType.valueOf(t.toUpperCase());
                    status = (s == null || s.trim().isEmpty()) ? null : TransactionStatus.valueOf(s.toUpperCase());
                    break;

                case 2:
                    search = consoleapp.readLine("Enter note text: ");
                    break;

                case 3:
                    List<Transaction> txs =
                            transactionService.getTransactionHistory(
                                    user, fromDate, toDate, type, status, search);

                    if (txs.isEmpty()) {
                        consoleapp.displayMessage("No transactions found.");
                    } else {
                        txs.forEach(tx -> consoleapp.displayMessage(
                                tx.getCreatedAt() + " | " +
                                        tx.getTransactionType() + " | " +
                                        tx.getAmount() + " | " +
                                        tx.getStatus() + " | " +
                                        (tx.getNote() != null ? tx.getNote() : "")
                        ));
                    }
                    break;

                case 4:
                    String path = consoleapp.readLine("Enter file name (example: txns.csv): ");
                    List<Transaction> exportTxs =
                            transactionService.getTransactionHistory(
                                    user, fromDate, toDate, type, status, search);

                    transactionService.exportTransactionHistory(exportTxs, path);
                    consoleapp.displayMessage("Transaction history exported successfully.");
                    break;

                case 0:
                    return;

                default:
                    consoleapp.displayError("Invalid option");
            }
        }
    }


}
