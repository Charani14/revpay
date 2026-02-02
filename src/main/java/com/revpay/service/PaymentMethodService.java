package com.revpay.service;

import com.revpay.entity.PaymentMethod;
import com.revpay.entity.User;
import com.revpay.entity.enums.PaymentMethodType;
import com.revpay.repository.PaymentMethodRepository;
import com.revpay.security.EncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentMethodService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    /* ================= ADD CARD ================= */

    public void addCard(User user,
                        String cardNumber,
                        String cardType,
                        String expiryDate,
                        boolean makeDefault) {

        PaymentMethod pm = new PaymentMethod();
        pm.setUser(user);
        pm.setPaymentMethodType(PaymentMethodType.CARD);
        pm.setEncryptedCardNumber(encryptionUtil.encrypt(cardNumber));
        pm.setCardType(cardType);
        pm.setExpiryDate(expiryDate);

        if (makeDefault) {
            unsetExistingDefault(user);
            pm.setDefault(true);
        }

        paymentMethodRepository.save(pm);
    }

    /* ================= ADD BANK ================= */

    public void addBankAccount(User user,
                               String accountNumber,
                               String bankName,
                               boolean makeDefault) {

        PaymentMethod pm = new PaymentMethod();
        pm.setUser(user);
        pm.setPaymentMethodType(PaymentMethodType.BANK_ACCOUNT);
        pm.setEncryptedBankAccountNumber(encryptionUtil.encrypt(accountNumber));
        pm.setBankName(bankName);

        if (makeDefault) {
            unsetExistingDefault(user);
            pm.setDefault(true);
        }

        paymentMethodRepository.save(pm);
    }

    /* ================= VIEW METHODS ================= */

    public List<PaymentMethod> getPaymentMethods(User user) {
        return paymentMethodRepository.findByUser(user);
    }

    /* ================= REMOVE METHOD ================= */

    public void removePaymentMethod(User user, Long methodId) {
        PaymentMethod pm = paymentMethodRepository
                .findByIdAndUser(methodId, user)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        paymentMethodRepository.delete(pm);
    }

    /* ================= SET DEFAULT ================= */

    public void setDefaultPaymentMethod(User user, Long methodId) {

        PaymentMethod pm = paymentMethodRepository
                .findByIdAndUser(methodId, user)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));

        unsetExistingDefault(user);
        pm.setDefault(true);
        paymentMethodRepository.save(pm);
    }

    /* ================= HELPER ================= */

    private void unsetExistingDefault(User user) {
        List<PaymentMethod> methods = paymentMethodRepository.findByUser(user);
        for (PaymentMethod m : methods) {
            if (m.isDefault()) {
                m.setDefault(false);
                paymentMethodRepository.save(m);
            }
        }
    }
}
