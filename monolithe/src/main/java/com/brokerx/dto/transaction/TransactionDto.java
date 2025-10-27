package com.brokerx.dto.transaction;

import com.brokerx.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionDto {
    private UUID transactionId;
    private UUID walletId;
    private String idempotencyKey;
    private BigDecimal amount;
    private String transactionType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
    private String failureReason;

    public TransactionDto(Transaction transaction) {
        this.transactionId = transaction.getTransactionId();
        this.walletId = transaction.getWallet().getWalletId();
        this.idempotencyKey = transaction.getIdempotencyKey();
        this.amount = transaction.getAmount();
        this.transactionType = transaction.getTransactionType();
        this.status = transaction.getStatus();
        this.createdAt = transaction.getCreatedAt();
        this.settledAt = transaction.getSettledAt();
        this.failureReason = transaction.getFailureReason();
    }

    // Getters
    public UUID getTransactionId() { return transactionId; }
    public UUID getWalletId() { return walletId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getTransactionType() { return transactionType; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public String getFailureReason() { return failureReason; }
}