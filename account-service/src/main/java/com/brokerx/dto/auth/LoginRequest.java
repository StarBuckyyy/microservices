package com.brokerx.dto.auth;

public class LoginRequest {
    private String email;
    private String hashedPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }
}
