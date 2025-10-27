package com.brokerx.controller;

import com.brokerx.entity.Account;
import com.brokerx.service.AccountService;
import com.brokerx.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class OtpController {

    private final AccountService accountService;
    private final OtpService otpService;

    public OtpController(AccountService accountService, OtpService otpService) {
        this.accountService = accountService;
        this.otpService = otpService;
    }

    // Générer un OTP pour un compte PENDING
    @PostMapping("/{accountId}/generate-otp")
    public ResponseEntity<String> generateOtp(@PathVariable UUID accountId) {
        Account account = accountService.getAccountById(accountId);
        if (account == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(account.getStatus())) return ResponseEntity.badRequest().body("Compte déjà actif");

        String otp = otpService.generateOtp(account);
        return ResponseEntity.ok("OTP généré: " + otp);
    }

    // Vérifier l’OTP
    @PostMapping("/{accountId}/verify-otp")
    public ResponseEntity<String> verifyOtp(@PathVariable UUID accountId, @RequestParam String otp) {
        Account account = accountService.getAccountById(accountId);
        if (account == null) return ResponseEntity.notFound().build();
        if (!"PENDING".equals(account.getStatus())) return ResponseEntity.badRequest().body("Compte déjà actif");

        boolean valid = otpService.verifyOtp(account, otp);
        if (valid) return ResponseEntity.ok("Compte activé !");
        return ResponseEntity.badRequest().body("OTP invalide ou expiré");
    }
}
