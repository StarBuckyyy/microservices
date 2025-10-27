package com.brokerx.controller;

import com.brokerx.dto.wallet.WalletDto;
import com.brokerx.dto.payment.PaymentResult;
import com.brokerx.entity.Wallet;
import com.brokerx.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;

import com.brokerx.dto.CreateWalletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
public class WalletController {
    
    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);
    private final WalletService walletService;
    private final RestTemplate restTemplate;
    
    private static final String ACCOUNT_SERVICE_URL = "http://account-service:8081";

    public WalletController(WalletService walletService, RestTemplate restTemplate) {
        this.walletService = walletService;
        this.restTemplate = restTemplate;
    }
    
    @GetMapping
    public List<WalletDto> getAllWallets() {
        return walletService.getAllWallets()
                            .stream()
                            .map(WalletDto::new)
                            .toList();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createWalletForAccount(@RequestBody CreateWalletRequest request) {
        logger.info("Received request to create wallet for accountId: {}", request.getAccountId());
        walletService.createWallet(request.getAccountId());
    }
    
    @GetMapping("/my-wallet")
    public ResponseEntity<WalletDto> getMyWallet(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        
        try {
            // ✅ Appel HTTP vers account-service pour récupérer le User
            String userUrl = ACCOUNT_SERVICE_URL + "/users/email/" + email;
            Map<String, Object> user = restTemplate.getForObject(userUrl, Map.class);
            
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            UUID userId = UUID.fromString((String) user.get("userId"));
            
            // ✅ Appel HTTP pour récupérer le Account
            String accountUrl = ACCOUNT_SERVICE_URL + "/accounts/user/" + userId;
            Map<String, Object> account = restTemplate.getForObject(accountUrl, Map.class);
            
            if (account == null) {
                return ResponseEntity.notFound().build();
            }
            
            UUID accountId = UUID.fromString((String) account.get("accountId"));
            
            Wallet wallet = walletService.getWalletByAccountId(accountId);
            if (wallet == null) {
                String status = (String) account.get("status");
                if ("ACTIVE".equals(status)) {
                    wallet = walletService.createWallet(accountId);
                    logger.info("Created new wallet on-the-fly for account: {}", accountId);
                    return ResponseEntity.ok(new WalletDto(wallet));
                }
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new WalletDto(wallet));
            
        } catch (Exception e) {
            logger.error("Error fetching wallet for user: {}", email, e);
            return ResponseEntity.status(500).build();
        }
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
    @GetMapping("/account/{accountId}")
    public ResponseEntity<WalletDto> getWalletByAccountId(@PathVariable UUID accountId) {
        logger.info("Internal request to find wallet for accountId: {}", accountId);
        Wallet wallet = walletService.getWalletByAccountId(accountId);
        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new WalletDto(wallet));
    }
}