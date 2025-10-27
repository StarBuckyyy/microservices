package com.brokerx.service;

import com.brokerx.entity.Account;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private final AccountService accountService;

    public OtpService(AccountService accountService) {
        this.accountService = accountService;
    }

    public String generateOtp(Account account) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        account.setVerificationToken(otp);
        account.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
        accountService.saveAccount(account); 
        return otp;
    }

    // Vérifie l’OTP
    public boolean verifyOtp(Account account, String otp) {
        if (account.getVerificationToken() == null) return false;
        if (!account.getVerificationToken().equals(otp)) return false;
        if (account.getTokenExpiresAt() == null || account.getTokenExpiresAt().isBefore(LocalDateTime.now())) return false;

        account.setStatus("ACTIVE");
        account.setVerificationToken(null);
        account.setTokenExpiresAt(null);
        account.setVerifiedAt(LocalDateTime.now());

        accountService.saveAccount(account);

        return true;
    }
}
