package com.brokerx.service;

import com.brokerx.wallet.dto.audit.CreateAuditLogRequest;
import com.brokerx.entity.Wallet;
import com.brokerx.repository.WalletRepository;
import com.brokerx.entity.Transaction;
import com.brokerx.repository.TransactionRepository;
import com.brokerx.dto.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;
    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final AuditService auditService; // ✅ AJOUT

    private static final String ACCOUNT_SERVICE_URL = "http://account-service:8081";

    public WalletService(WalletRepository walletRepository, PaymentService paymentService, 
                        TransactionRepository transactionRepository, RestTemplate restTemplate,
                        AuditService auditService) { // ✅ AJOUT
        this.walletRepository = walletRepository;
        this.paymentService = paymentService;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
        this.auditService = auditService; // ✅ AJOUT
    }

    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    public Wallet createWallet(UUID accountId) {
        boolean accountExists = checkAccountExists(accountId);
        if (!accountExists) {
            throw new RuntimeException("Account not found: " + accountId);
        }

        Wallet wallet = new Wallet();
        wallet.setAccountId(accountId);
        wallet.setCurrency("USD");
        wallet.setBalance(BigDecimal.ZERO);
        
        Wallet savedWallet = walletRepository.save(wallet);
        logger.info("Wallet created: walletId={}, accountId={}", savedWallet.getWalletId(), accountId);
        
        // ✅ AUDIT : Log création wallet
        auditService.logAction(
            "WALLET", 
            savedWallet.getWalletId(), 
            "CREATE", 
            null, // Pas d'utilisateur direct
            null,
            Map.of(
                "accountId", accountId.toString(),
                "initialBalance", "0.00",
                "currency", "USD"
            )
        );
        
        return savedWallet;
    }

    private void logAudit(String entityType, UUID entityId, String action, UUID accountId, Map<String, Object> details) {
        try {
            Map accountDetails = restTemplate.getForObject("http://account-service:8081/accounts/" + accountId, Map.class);
            UUID userId = UUID.fromString((String) accountDetails.get("userId"));

            CreateAuditLogRequest logRequest = new CreateAuditLogRequest();
            logRequest.entityType = entityType;
            logRequest.entityId = entityId;
            logRequest.action = action;
            logRequest.performedBy = userId;
            logRequest.ipAddress = "internal-wallet-service";
            logRequest.details = details;

            restTemplate.postForObject("http://account-service:8081/audit", logRequest, Void.class);
        } catch (Exception e) {
            logger.error("Failed to send audit log for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    @Transactional
    public PaymentResult deposit(UUID walletId, Double amount, String paymentMethod) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé"));
        
        BigDecimal oldBalance = wallet.getBalance();
        
        PaymentResult paymentResult = paymentService.processDeposit(
            walletId, BigDecimal.valueOf(amount), paymentMethod);
        
        if (paymentResult.isSuccess()) {
            wallet.setBalance(wallet.getBalance().add(paymentResult.getAmount()));
            walletRepository.save(wallet);
            
            Transaction transaction = new Transaction();
            transaction.setWallet(wallet);
            transaction.setAmount(paymentResult.getAmount());
            transaction.setTransactionType("DEPOSIT");
            transaction.setStatus("SETTLED");
            transaction.setIdempotencyKey(paymentResult.getTransactionId().toString());
            Transaction savedTx = transactionRepository.save(transaction);
            
            // ✅ AUDIT : Log dépôt
            auditService.logAction(
                "TRANSACTION", 
                savedTx.getTransactionId(), 
                "DEPOSIT", 
                null, // Pourrait être l'userId si disponible
                null,
                Map.of(
                    "walletId", walletId.toString(),
                    "amount", paymentResult.getAmount().toString(),
                    "oldBalance", oldBalance.toString(),
                    "newBalance", wallet.getBalance().toString(),
                    "paymentMethod", paymentMethod
                )
            );
            
            logger.info("Deposit successful: walletId={}, amount={}, newBalance={}", 
                       walletId, paymentResult.getAmount(), wallet.getBalance());
        }
        
        return paymentResult;
    }

    @Transactional
    public PaymentResult withdraw(UUID walletId, Double amount, String paymentMethod) {
        logger.info("Initiating withdrawal: walletId={}, amount={}", walletId, amount);
        
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé"));
        
        BigDecimal withdrawAmount = BigDecimal.valueOf(amount);
        BigDecimal oldBalance = wallet.getBalance();
        
        if (wallet.getBalance().compareTo(withdrawAmount) < 0) {
            return PaymentResult.failure("INSUFFICIENT_FUNDS", "Fonds insuffisants");
        }
        
        PaymentResult paymentResult = paymentService.processWithdrawal(
            walletId, withdrawAmount, paymentMethod);
        
        if (paymentResult.isSuccess()) {
            BigDecimal newBalance = wallet.getBalance().subtract(paymentResult.getAmount());
            wallet.setBalance(newBalance);
            walletRepository.save(wallet);
            
            Transaction transaction = new Transaction();
            transaction.setWallet(wallet);
            transaction.setAmount(paymentResult.getAmount());
            transaction.setTransactionType("WITHDRAWAL");
            transaction.setStatus("SETTLED");
            transaction.setIdempotencyKey(paymentResult.getTransactionId().toString());
            Transaction savedTx = transactionRepository.save(transaction);
            
            // ✅ AUDIT : Log retrait
            auditService.logAction(
                "TRANSACTION", 
                savedTx.getTransactionId(), 
                "WITHDRAWAL", 
                null,
                null,
                Map.of(
                    "walletId", walletId.toString(),
                    "amount", paymentResult.getAmount().toString(),
                    "oldBalance", oldBalance.toString(),
                    "newBalance", newBalance.toString(),
                    "paymentMethod", paymentMethod
                )
            );
            
            logger.info("Withdrawal completed: walletId={}, amount={}, newBalance={}", 
                       walletId, paymentResult.getAmount(), newBalance);
        }
        
        return paymentResult;
    }

    public Wallet getWalletByAccountId(UUID accountId) {
        return walletRepository.findByAccountId(accountId).orElse(null);
    }

    private boolean checkAccountExists(UUID accountId) {
        try {
            String url = ACCOUNT_SERVICE_URL + "/accounts/" + accountId;
            restTemplate.getForObject(url, Map.class);
            return true;
        } catch (Exception e) {
            logger.error("Account not found or unreachable: {}", accountId, e);
            return false;
        }
    }
}