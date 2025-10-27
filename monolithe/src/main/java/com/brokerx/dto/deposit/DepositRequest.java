package com.brokerx.dto.deposit;

import java.util.UUID;
import java.time.LocalDateTime;



public class DepositRequest {
    private UUID walletId;
    private Double amount;

    // getters et setters
    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
