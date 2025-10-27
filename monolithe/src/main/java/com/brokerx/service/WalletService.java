package com.brokerx.service;

import com.brokerx.entity.Account;
import com.brokerx.entity.Wallet;
import com.brokerx.repository.WalletRepository;
import com.brokerx.entity.Transaction;
import com.brokerx.repository.TransactionRepository;
import com.brokerx.dto.payment.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;
    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService; // ✅ AJOUT

    public WalletService(WalletRepository walletRepository, PaymentService paymentService, 
                        TransactionRepository transactionRepository, AuditService auditService) {
        this.walletRepository = walletRepository;
        this.paymentService = paymentService;
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
    }

    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    public Wallet createWallet(Account account) {
        Wallet wallet = new Wallet();
        wallet.setAccount(account);
        wallet.setCurrency("USD"); 
        wallet.setBalance(BigDecimal.ZERO);
        Wallet savedWallet = walletRepository.save(wallet);
        
        // ✅ AUDIT : Log création wallet
        auditService.logAction(
            "WALLET",
            savedWallet.getWalletId(),
            "CREATE",
            account.getUser().getUserId(),
            "127.0.0.1",
            Map.of(
                "accountId", account.getAccountId().toString(),
                "currency", "USD",
                "initialBalance", "0.00"
            )
        );
        
        return savedWallet;
    }

    @Transactional
    public PaymentResult deposit(UUID walletId, Double amount) {
        return deposit(walletId, amount, "CARD");
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
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            // ✅ AUDIT : Log dépôt
            auditService.logAction(
                "TRANSACTION",
                savedTransaction.getTransactionId(),
                "CREATE",
                wallet.getAccount().getUser().getUserId(),
                "127.0.0.1",
                Map.of(
                    "type", "DEPOSIT",
                    "amount", paymentResult.getAmount().toString(),
                    "oldBalance", oldBalance.toString(),
                    "newBalance", wallet.getBalance().toString(),
                    "paymentMethod", paymentMethod
                )
            );
        }
        
        return paymentResult;
    }

    @Transactional
    public PaymentResult withdraw(UUID walletId, Double amount) {
        return withdraw(walletId, amount, "BANK_TRANSFER");
    }

    @Transactional
    public PaymentResult withdraw(UUID walletId, Double amount, String paymentMethod) {
        logger.info("Initiating withdrawal: walletId={}, amount={}, paymentMethod={}", 
                   walletId, amount, paymentMethod);
        
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé"));
        
        BigDecimal withdrawAmount = BigDecimal.valueOf(amount);
        BigDecimal oldBalance = wallet.getBalance();
        
        if (wallet.getBalance().compareTo(withdrawAmount) < 0) {
            logger.warn("Insufficient funds: walletId={}, balance={}, requestedAmount={}", 
                       walletId, wallet.getBalance(), withdrawAmount);
            return PaymentResult.failure("INSUFFICIENT_FUNDS", "Fonds insuffisants");
        }
        
        PaymentResult paymentResult = paymentService.processWithdrawal(
            walletId, 
            withdrawAmount, 
            paymentMethod
        );
        
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
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            // ✅ AUDIT : Log retrait
            auditService.logAction(
                "TRANSACTION",
                savedTransaction.getTransactionId(),
                "CREATE",
                wallet.getAccount().getUser().getUserId(),
                "127.0.0.1",
                Map.of(
                    "type", "WITHDRAWAL",
                    "amount", paymentResult.getAmount().toString(),
                    "oldBalance", oldBalance.toString(),
                    "newBalance", newBalance.toString(),
                    "paymentMethod", paymentMethod
                )
            );
            
            logger.info("Withdrawal completed successfully: walletId={}, newBalance={}, transactionId={}", 
                       walletId, newBalance, paymentResult.getTransactionId());
        } else {
            logger.warn("Withdrawal failed: walletId={}, reason={}", walletId, paymentResult.getMessage());
        }
        
        return paymentResult;
    }

    public Wallet getWalletByAccountId(UUID accountId) {
        return walletRepository.findByAccount_AccountId(accountId)
                               .orElse(null);
    }
}