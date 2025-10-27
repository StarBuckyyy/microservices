package com.brokerx.service;

import com.brokerx.dto.order.OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class OrderValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderValidationService.class);
    
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("10000.00");
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 100000;
    private static final BigDecimal MAX_ORDER_VALUE = new BigDecimal("100000.00");
    
    private static final List<String> ALLOWED_SYMBOLS = Arrays.asList(
        "AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"
    );

    /**
     * Valide les champs de base d'un ordre (sans appels HTTP)
     */
    public ValidationResult validateBasicFields(OrderRequest request) {
        logger.debug("Validating basic fields for order: {}", request.getClientOrderId());

        // Validation du symbole
        if (request.getSymbol() == null || request.getSymbol().trim().isEmpty()) {
            return ValidationResult.failure("Symbol is required");
        }
        
        if (!ALLOWED_SYMBOLS.contains(request.getSymbol().toUpperCase())) {
            return ValidationResult.failure("Symbol not allowed: " + request.getSymbol());
        }
        
        // Validation du side
        if (request.getSide() == null || 
            (!request.getSide().equals("BUY") && !request.getSide().equals("SELL"))) {
            return ValidationResult.failure("Side must be BUY or SELL");
        }
        
        // Validation du type d'ordre
        if (request.getOrderType() == null || 
            (!request.getOrderType().equals("MARKET") && !request.getOrderType().equals("LIMIT"))) {
            return ValidationResult.failure("Order type must be MARKET or LIMIT");
        }
        
        // Validation de la quantité
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return ValidationResult.failure("Quantity must be positive");
        }
        
        if (request.getQuantity() < MIN_QUANTITY || request.getQuantity() > MAX_QUANTITY) {
            return ValidationResult.failure(
                String.format("Quantity must be between %d and %d", MIN_QUANTITY, MAX_QUANTITY)
            );
        }
        
        // Validation du prix pour LIMIT
        if ("LIMIT".equals(request.getOrderType())) {
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return ValidationResult.failure("Price is required for LIMIT orders");
            }
            
            if (request.getPrice().compareTo(MIN_PRICE) < 0) {
                return ValidationResult.failure("Price below minimum: " + MIN_PRICE);
            }
            
            if (request.getPrice().compareTo(MAX_PRICE) > 0) {
                return ValidationResult.failure("Price above maximum: " + MAX_PRICE);
            }
            
            // Vérifier que le prix est un multiple de 0.01
            BigDecimal remainder = request.getPrice().remainder(new BigDecimal("0.01"));
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                return ValidationResult.failure("Price must be multiple of 0.01");
            }
            
            // Vérifier la valeur totale de l'ordre
            BigDecimal orderValue = new BigDecimal(request.getQuantity()).multiply(request.getPrice());
            if (orderValue.compareTo(MAX_ORDER_VALUE) > 0) {
                return ValidationResult.failure("Order value exceeds maximum: " + MAX_ORDER_VALUE);
            }
        }
        
        // Validation du Time In Force
        if (request.getTimeInForce() == null || 
            !Arrays.asList("DAY", "IOC", "FOK").contains(request.getTimeInForce())) {
            return ValidationResult.failure("Time in force must be DAY, IOC, or FOK");
        }

        logger.debug("Basic validation passed for order: {}", request.getClientOrderId());
        return ValidationResult.success("Basic validation passed");
    }

    /**
     * Classe pour encapsuler le résultat de validation
     */
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

        public boolean isValid() { 
            return valid; 
        }
        
        public String getMessage() { 
            return message; 
        }
    }
}