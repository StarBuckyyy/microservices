package com.brokerx.dto.auth;

public class MfaVerifyRequest {
    private String tempToken;
    private String otpCode;

    public String getTempToken() { return tempToken; }
    public void setTempToken(String tempToken) { this.tempToken = tempToken; }
    
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}