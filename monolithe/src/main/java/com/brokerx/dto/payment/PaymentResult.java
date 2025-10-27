package com.brokerx.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentResult {
    private boolean success;
    private UUID transactionId;
    private BigDecimal amount;
    private String status;
    private String message;
    private LocalDateTime processedAt;
    private String paymentMethod;

    private PaymentResult(boolean success, UUID transactionId, BigDecimal amount, 
                         String status, String message, String paymentMethod) {
        this.success = success;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.message = message;
        this.paymentMethod = paymentMethod;
        this.processedAt = LocalDateTime.now();
    }

    public static PaymentResult success(UUID transactionId, BigDecimal amount, String status, String message) {
        return new PaymentResult(true, transactionId, amount, status, message, "SIMULATED");
    }

    public static PaymentResult failure(String status, String message) {
        return new PaymentResult(false, null, BigDecimal.ZERO, status, message, "SIMULATED");
    }

    // Getters
    public boolean isSuccess() { return success; }
    public UUID getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public String getPaymentMethod() { return paymentMethod; }
}