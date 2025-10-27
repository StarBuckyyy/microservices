package com.brokerx.service;

import com.brokerx.dto.CreateWalletRequest;
import com.brokerx.entity.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final AccountService accountService;
    private final RestTemplate restTemplate;

    public OtpService(AccountService accountService, RestTemplate restTemplate) {
        this.accountService = accountService;
        this.restTemplate = restTemplate;
    }

  
    public String generateOtp(Account account) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        account.setVerificationToken(otp);
        account.setTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
        
        this.accountService.saveAccount(account); 
        
        logger.info("OTP generated for account: {}, code: {}", account.getAccountId(), otp);
        return otp;
    }

    public boolean verifyOtp(Account account, String otp) {
        if (account.getVerificationToken() == null || !account.getVerificationToken().equals(otp)) {
            logger.warn("Invalid OTP '{}' for account {}", otp, account.getAccountId());
            return false;
        }
        if (account.getTokenExpiresAt() == null || account.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Expired OTP '{}' for account {}", otp, account.getAccountId());
            return false;
        }
        account.setStatus("ACTIVE");
        account.setVerificationToken(null);
        account.setTokenExpiresAt(null);
        account.setVerifiedAt(LocalDateTime.now());
        
        this.accountService.saveAccount(account);
        logger.info("Account {} activated successfully.", account.getAccountId());

        try {
            logger.info("Notifying wallet-service to create a wallet for accountId: {}", account.getAccountId());
            CreateWalletRequest request = new CreateWalletRequest(account.getAccountId());
            
            String walletServiceUrl = "http://wallet-service:8082/wallets";
            
            this.restTemplate.postForObject(walletServiceUrl, request, Void.class);

            logger.info("Successfully sent wallet creation request to wallet-service for accountId: {}", account.getAccountId());
        } catch (Exception e) {
            logger.error("!!! CRITICAL FAILURE !!! Could not notify wallet-service to create wallet for account {}. Error: {}", account.getAccountId(), e.getMessage());
        }
        
        return true;
    }
}