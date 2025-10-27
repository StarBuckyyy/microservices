package com.brokerx.dto;
import java.util.UUID;
public class CreateWalletRequest {
    private UUID accountId;
    public CreateWalletRequest() {}
    public CreateWalletRequest(UUID accountId) { this.accountId = accountId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
}