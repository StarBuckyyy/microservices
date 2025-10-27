package com.brokerx.dto.wallet;

import java.math.BigDecimal;
import java.util.UUID;

public class WalletDto {
    private UUID walletId;
    private UUID accountId;
    private BigDecimal balance;
    private String currency;

    public WalletDto() {}

    public UUID getWalletId() { return walletId; }
    public UUID getAccountId() { return accountId; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
}