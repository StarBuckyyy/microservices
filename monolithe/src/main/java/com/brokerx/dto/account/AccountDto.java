package com.brokerx.dto.account;

import com.brokerx.entity.Account;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountDto {
    private UUID accountId;
    private String status;
    private UUID userId;
    private LocalDateTime verifiedAt;
    private String verificationToken;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AccountDto(Account account) {
        this.accountId = account.getAccountId();
        this.status = account.getStatus();
        this.userId = account.getUser() != null ? account.getUser().getUserId() : null;
        this.verifiedAt = account.getVerifiedAt();
        this.verificationToken = account.getVerificationToken();
        this.tokenExpiresAt = account.getTokenExpiresAt();
        this.createdAt = account.getCreatedAt();
        this.updatedAt = account.getUpdatedAt();
    }

    // Getters
    public UUID getAccountId() { return accountId; }
    public String getStatus() { return status; }
    public UUID getUserId() { return userId; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public String getVerificationToken() { return verificationToken; }
    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
