package com.brokerx.dto.transaction;

import com.brokerx.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionDto {
    private UUID transactionId;
    private UUID walletId;
    private BigDecimal amount;
    private String transactionType;
    private String status;
    private LocalDateTime createdAt;
    
    public TransactionDto() {}
    
    public TransactionDto(Transaction transaction) {
        this.transactionId = transaction.getTransactionId();
        this.walletId = transaction.getWallet().getWalletId();
        this.amount = transaction.getAmount();
        this.transactionType = transaction.getTransactionType();
        this.status = transaction.getStatus();
        this.createdAt = transaction.getCreatedAt();
    }
    
    // Getters/Setters
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}