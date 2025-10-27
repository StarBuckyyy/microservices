package com.brokerx.service;

import com.brokerx.dto.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Service
public class SimulatedPaymentService implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(SimulatedPaymentService.class);
    private final Random random = new Random();

    @Override
    public PaymentResult processDeposit(UUID walletId, BigDecimal amount, String paymentMethod) {
        logger.info("Processing deposit: walletId={}, amount={}, paymentMethod={}", 
                   walletId, amount, paymentMethod);
        
        simulateProcessingDelay();
        
        UUID transactionId = UUID.randomUUID();
        
        logger.info("Deposit processed successfully: transactionId={}, walletId={}, amount={}", 
                   transactionId, walletId, amount);
        
        return PaymentResult.success(
            transactionId,
            amount,
            "DEPOSIT_COMPLETED",
            "Deposit processed successfully via simulated payment gateway"
        );
    }

    @Override
    public PaymentResult processWithdrawal(UUID walletId, BigDecimal amount, String paymentMethod) {
        logger.info("Processing withdrawal: walletId={}, amount={}, paymentMethod={}", 
                   walletId, amount, paymentMethod);
        
        simulateProcessingDelay();
        
        UUID transactionId = UUID.randomUUID();
        
        logger.info("Withdrawal processed successfully: transactionId={}, walletId={}, amount={}", 
                   transactionId, walletId, amount);
        
        return PaymentResult.success(
            transactionId,
            amount,
            "WITHDRAWAL_COMPLETED",
            "Withdrawal processed successfully via simulated payment gateway"
        );
    }

    private void simulateProcessingDelay() {
        try {
            int delay = 500 + random.nextInt(1500);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Payment processing simulation interrupted", e);
        }
    }

    public PaymentResult simulateFailure(String reason) {
        return PaymentResult.failure("PAYMENT_FAILED", reason);
    }
}