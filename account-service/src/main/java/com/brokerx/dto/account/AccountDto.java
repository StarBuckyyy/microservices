package com.brokerx.dto.account;

import com.brokerx.entity.Account;
import java.util.UUID;
import java.time.LocalDateTime;

public class AccountDto {
    private UUID accountId;
    private UUID userId;
    private String status;
    private LocalDateTime createdAt;
    
    public AccountDto() {}
    
    public AccountDto(Account account) {
        this.accountId = account.getAccountId();
        this.userId = account.getUser().getUserId();
        this.status = account.getStatus();
        this.createdAt = account.getCreatedAt();
    }
    
    // Getters/Setters
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}