package com.brokerx.dto.wallet;

import com.brokerx.entity.Wallet;
import java.math.BigDecimal;
import java.util.UUID;

public class WalletDto {
    private UUID walletId;
    private UUID accountId;
    private BigDecimal balance;
    private String currency;
    
    public WalletDto() {}
    
    public WalletDto(Wallet wallet) {
        this.walletId = wallet.getWalletId();
        this.accountId = wallet.getAccountId();
        this.balance = wallet.getBalance();
        this.currency = wallet.getCurrency();
    }
    
    // Getters/Setters
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}