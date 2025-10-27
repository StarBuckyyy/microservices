package com.brokerx.service;

import com.brokerx.dto.payment.PaymentResult;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MockPaymentService implements PaymentService {
    
    @Override
    public PaymentResult processDeposit(UUID walletId, BigDecimal amount, String paymentMethod) {
        PaymentResult result = new PaymentResult(true, "Deposit processed successfully");
        result.setStatus("SUCCESS");
        result.setTransactionId(UUID.randomUUID());
        result.setAmount(amount);
        result.setProcessedAt(LocalDateTime.now());
        return result;
    }
    
    @Override
    public PaymentResult processWithdrawal(UUID walletId, BigDecimal amount, String paymentMethod) {
        PaymentResult result = new PaymentResult(true, "Withdrawal processed successfully");
        result.setStatus("SUCCESS");
        result.setTransactionId(UUID.randomUUID());
        result.setAmount(amount);
        result.setProcessedAt(LocalDateTime.now());
        return result;
    }
}