package com.brokerx.controller;

import com.brokerx.dto.wallet.WalletDto;
import com.brokerx.dto.payment.PaymentResult;
import com.brokerx.entity.Account;
import com.brokerx.entity.Wallet;
import com.brokerx.service.AccountService;
import com.brokerx.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import com.brokerx.entity.User;
import com.brokerx.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
public class WalletController {
    
    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);
    private final WalletService walletService;
    private final AccountService accountService;
    private final UserService userService;

    public WalletController(WalletService walletService, AccountService accountService, UserService userService) {
        this.walletService = walletService;
        this.accountService = accountService;
        this.userService = userService;
    }
    
    @GetMapping
    public List<WalletDto> getAllWallets() {
        return walletService.getAllWallets()
                            .stream()
                            .map(WalletDto::new)
                            .toList();
    }

    @GetMapping("/my-wallet")
    public ResponseEntity<WalletDto> getMyWallet(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Account account = accountService.getAccountByUserId(user.getUserId());
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        Wallet wallet = walletService.getWalletByAccountId(account.getAccountId());
        if (wallet == null) {
            if ("ACTIVE".equals(account.getStatus())) {
                wallet = walletService.createWallet(account);
                logger.info("Created new wallet on-the-fly for account: {}", account.getAccountId());
                return ResponseEntity.ok(new WalletDto(wallet));
            }
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new WalletDto(wallet));
    }
    

    @PostMapping("/{accountId}")
    public ResponseEntity<WalletDto> createWallet(@PathVariable UUID accountId) {
        Account account = accountService.getAccountById(accountId);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        Wallet wallet = walletService.createWallet(account);
        return ResponseEntity.ok(new WalletDto(wallet));
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestParam String walletId, 
                                                       @RequestParam Double amount,
                                                       @RequestParam(defaultValue = "CARD") String paymentMethod) {
        try {
            UUID uuid = UUID.fromString(walletId);
            
            logger.info("Deposit request received: walletId={}, amount={}, paymentMethod={}", 
                       walletId, amount, paymentMethod);
            
            PaymentResult result = walletService.deposit(uuid, amount, paymentMethod);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("status", result.getStatus());
            
            if (result.isSuccess()) {
                response.put("transactionId", result.getTransactionId());
                response.put("amount", result.getAmount());
                response.put("processedAt", result.getProcessedAt());
                
                logger.info("Deposit successful: transactionId={}", result.getTransactionId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Deposit failed: walletId={}, reason={}", walletId, result.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid wallet ID: {}", walletId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Wallet ID invalide");
            errorResponse.put("status", "INVALID_WALLET_ID");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during deposit: walletId={}, amount={}", walletId, amount, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur système: " + e.getMessage());
            errorResponse.put("status", "SYSTEM_ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestParam String walletId, 
                                                        @RequestParam Double amount,
                                                        @RequestParam(defaultValue = "BANK_TRANSFER") String paymentMethod) {
        try {
            UUID uuid = UUID.fromString(walletId);
            
            logger.info("Withdrawal request received: walletId={}, amount={}, paymentMethod={}", 
                       walletId, amount, paymentMethod);
            
            PaymentResult result = walletService.withdraw(uuid, amount, paymentMethod);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("status", result.getStatus());
            
            if (result.isSuccess()) {
                response.put("transactionId", result.getTransactionId());
                response.put("amount", result.getAmount());
                response.put("processedAt", result.getProcessedAt());
                
                logger.info("Withdrawal successful: transactionId={}", result.getTransactionId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Withdrawal failed: walletId={}, reason={}", walletId, result.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid wallet ID: {}", walletId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Wallet ID invalide");
            errorResponse.put("status", "INVALID_WALLET_ID");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during withdrawal: walletId={}, amount={}", walletId, amount, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur système: " + e.getMessage());
            errorResponse.put("status", "SYSTEM_ERROR");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}