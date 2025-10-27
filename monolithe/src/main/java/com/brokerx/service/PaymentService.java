package com.brokerx.service;

import com.brokerx.dto.payment.PaymentResult;
import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    PaymentResult processDeposit(UUID walletId, BigDecimal amount, String paymentMethod);
    PaymentResult processWithdrawal(UUID walletId, BigDecimal amount, String paymentMethod);
}