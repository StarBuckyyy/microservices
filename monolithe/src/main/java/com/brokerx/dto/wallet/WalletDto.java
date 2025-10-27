package com.brokerx.dto.wallet;

import com.brokerx.entity.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class WalletDto {
    private UUID walletId;
    private UUID accountId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WalletDto(Wallet wallet) {
        this.walletId = wallet.getWalletId();
        this.accountId = wallet.getAccount().getAccountId();
        this.balance = wallet.getBalance();
        this.currency = wallet.getCurrency();
        this.createdAt = wallet.getCreatedAt();
        this.updatedAt = wallet.getUpdatedAt();
    }

    // Getters
    public UUID getWalletId() { return walletId; }
    public UUID getAccountId() { return accountId; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
