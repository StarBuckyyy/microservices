package com.brokerx.service;

import com.brokerx.dto.order.OrderRequest;
import com.brokerx.entity.Account;
import com.brokerx.entity.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderValidationService.class);
    
    // Configuration des limites
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000.00");
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 100000;
    private static final BigDecimal MAX_ORDER_VALUE = new BigDecimal("100000.00");
    
    private static final List<String> ALLOWED_SYMBOLS = Arrays.asList("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN");

    
    public ValidationResult validateOrder(OrderRequest request, Account account, Wallet wallet) {
        return validateOrder(request, account, wallet, null);
    }

    
    public ValidationResult validateOrder(OrderRequest request, Account account, Wallet wallet, 
                                        FundReservationService fundReservationService) {
        logger.info("Validating order: clientOrderId={}, symbol={}, side={}, quantity={}", 
                   request.getClientOrderId(), request.getSymbol(), request.getSide(), request.getQuantity());

        ValidationResult basicValidation = validateBasicFields(request);
        if (!basicValidation.isValid()) {
            return basicValidation;
        }

        ValidationResult accountValidation = validateAccount(account);
        if (!accountValidation.isValid()) {
            return accountValidation;
        }

        ValidationResult instrumentValidation = validateInstrument(request);
        if (!instrumentValidation.isValid()) {
            return instrumentValidation;
        }

        ValidationResult priceValidation = validatePriceRules(request);
        if (!priceValidation.isValid()) {
            return priceValidation;
        }

        if ("BUY".equals(request.getSide())) {
            ValidationResult buyingPowerValidation = validateBuyingPower(request, wallet, fundReservationService);
            if (!buyingPowerValidation.isValid()) {
                return buyingPowerValidation;
            }
        }

        ValidationResult userLimitsValidation = validateUserLimits(request);
        if (!userLimitsValidation.isValid()) {
            return userLimitsValidation;
        }

        logger.info("Order validation successful: clientOrderId={}", request.getClientOrderId());
        return ValidationResult.success("Order validation passed");
    }

    private ValidationResult validateBasicFields(OrderRequest request) {
        if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
            return ValidationResult.failure("Symbol is required");
        }
        
        if (request.getSide() == null || (!request.getSide().equals("BUY") && !request.getSide().equals("SELL"))) {
            return ValidationResult.failure("Side must be BUY or SELL");
        }
        
        if (request.getOrderType() == null || (!request.getOrderType().equals("MARKET") && !request.getOrderType().equals("LIMIT"))) {
            return ValidationResult.failure("Order type must be MARKET or LIMIT");
        }
        
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return ValidationResult.failure("Quantity must be positive");
        }
        
        if ("LIMIT".equals(request.getOrderType()) && (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            return ValidationResult.failure("Price is required for LIMIT orders and must be positive");
        }
        
        if (request.getTimeInForce() == null || (!Arrays.asList("DAY", "IOC", "FOK").contains(request.getTimeInForce()))) {
            return ValidationResult.failure("Time in force must be DAY, IOC, or FOK");
        }

        return ValidationResult.success("Basic validation passed");
    }

    private ValidationResult validateAccount(Account account) {
        if (account == null) {
            return ValidationResult.failure("Account not found");
        }
        
        if (!"ACTIVE".equals(account.getStatus())) {
            return ValidationResult.failure("Account is not active");
        }

        return ValidationResult.success("Account validation passed");
    }

    private ValidationResult validateInstrument(OrderRequest request) {
        if (!ALLOWED_SYMBOLS.contains(request.getSymbol())) {
            return ValidationResult.failure("Symbol " + request.getSymbol() + " is not allowed. Allowed symbols: " + ALLOWED_SYMBOLS);
        }

        return ValidationResult.success("Instrument validation passed");
    }

    private ValidationResult validatePriceRules(OrderRequest request) {
        if ("LIMIT".equals(request.getOrderType())) {
            if (request.getPrice().compareTo(MIN_PRICE) < 0) {
                return ValidationResult.failure("Price below minimum allowed (" + MIN_PRICE + ")");
            }
            
            if (request.getPrice().compareTo(MAX_PRICE) > 0) {
                return ValidationResult.failure("Price above maximum allowed (" + MAX_PRICE + ")");
            }
            
            BigDecimal remainder = request.getPrice().remainder(new BigDecimal("0.01"));
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                return ValidationResult.failure("Price must be a multiple of 0.01 (tick size)");
            }
        }

        return ValidationResult.success("Price rules validation passed");
    }

    private ValidationResult validateBuyingPower(OrderRequest request, Wallet wallet, 
                                                FundReservationService fundReservationService) {
        if (wallet == null) {
            return ValidationResult.failure("Wallet not found");
        }

        BigDecimal requiredAmount;
        if ("MARKET".equals(request.getOrderType())) {
            // Pour les ordres MARKET, on estime le coût maximal 
            requiredAmount = new BigDecimal(request.getQuantity()).multiply(MAX_PRICE);
        } else {
            // Pour les ordres LIMIT, on calcule le coût exact
            requiredAmount = new BigDecimal(request.getQuantity()).multiply(request.getPrice());
        }

        BigDecimal availableBalance;
        if (fundReservationService != null) {
            availableBalance = fundReservationService.getAvailableBalance(wallet);
            BigDecimal totalReserved = fundReservationService.getTotalReserved(wallet.getWalletId());
            
            logger.info("Buying power check: walletBalance={}, totalReserved={}, availableBalance={}, required={}", 
                       wallet.getBalance(), totalReserved, availableBalance, requiredAmount);
        } else {
            availableBalance = wallet.getBalance();
            logger.info("Buying power check (no reservations): walletBalance={}, required={}", 
                       availableBalance, requiredAmount);
        }

        if (availableBalance.compareTo(requiredAmount) < 0) {
            return ValidationResult.failure(
                String.format("Insufficient buying power. Required: %s, Available: %s (after existing orders)", 
                             requiredAmount, availableBalance));
        }

        return ValidationResult.success("Buying power validation passed");
    }

    private ValidationResult validateUserLimits(OrderRequest request) {
        if (request.getQuantity() < MIN_QUANTITY || request.getQuantity() > MAX_QUANTITY) {
            return ValidationResult.failure("Quantity must be between " + MIN_QUANTITY + " and " + MAX_QUANTITY);
        }

        if ("LIMIT".equals(request.getOrderType())) {
            BigDecimal orderValue = new BigDecimal(request.getQuantity()).multiply(request.getPrice());
            if (orderValue.compareTo(MAX_ORDER_VALUE) > 0) {
                return ValidationResult.failure("Order value exceeds maximum allowed (" + MAX_ORDER_VALUE + ")");
            }
        }

        return ValidationResult.success("User limits validation passed");
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }
}