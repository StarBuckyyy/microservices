package com.brokerx.dto.auth;

import java.util.UUID;

public class OtpVerifyRequest {
    private UUID accountId;
    private String otp;

    public UUID getAccountId() { 
        return accountId; 
    }
    
    public void setAccountId(UUID accountId) { 
        this.accountId = accountId; 
    }

    public String getOtp() { 
        return otp; 
    }
    
    public void setOtp(String otp) { 
        this.otp = otp; 
    }
}