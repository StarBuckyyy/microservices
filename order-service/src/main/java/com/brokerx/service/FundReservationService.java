package com.brokerx.service;

import com.brokerx.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FundReservationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FundReservationService.class);
    
    private final Map<UUID, BigDecimal> reservations = new ConcurrentHashMap<>();
    private final Map<UUID, BigDecimal> totalReservationsByWallet = new ConcurrentHashMap<>();
    
    public boolean reserveFunds(UUID orderId, UUID walletId, BigDecimal amount) {
        logger.info("Reserving funds: orderId={}, walletId={}, amount={}", orderId, walletId, amount);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        if (reservations.containsKey(orderId)) {
            return false;
        }
        
        reservations.put(orderId, amount);
        totalReservationsByWallet.merge(walletId, amount, BigDecimal::add);
        
        logger.info("Funds reserved: orderId={}, amount={}", orderId, amount);
        return true;
    }
    
    public void releaseFunds(UUID orderId, UUID walletId) {
        BigDecimal reservedAmount = reservations.remove(orderId);
        
        if (reservedAmount != null) {
            totalReservationsByWallet.merge(walletId, reservedAmount.negate(), BigDecimal::add);
            
            if (totalReservationsByWallet.get(walletId).compareTo(BigDecimal.ZERO) == 0) {
                totalReservationsByWallet.remove(walletId);
            }
            
            logger.info("Funds released: orderId={}, amount={}", orderId, reservedAmount);
        }
    }
    
    public BigDecimal getTotalReserved(UUID walletId) {
        return totalReservationsByWallet.getOrDefault(walletId, BigDecimal.ZERO);
    }
    
    public BigDecimal getReservedAmount(UUID orderId) {
        return reservations.getOrDefault(orderId, BigDecimal.ZERO);
    }
    
    public BigDecimal calculateReservationAmount(Order order) {
        if (!"BUY".equals(order.getSide())) {
            return BigDecimal.ZERO;
        }
        
        if ("MARKET".equals(order.getOrderType())) {
            return new BigDecimal(order.getQuantity()).multiply(new BigDecimal("100.00"));
        } else if ("LIMIT".equals(order.getOrderType())) {
            return new BigDecimal(order.getQuantity()).multiply(order.getPrice());
        }
        
        return BigDecimal.ZERO;
    }
    
    public Map<String, Object> getReservationStats() {
        return Map.of(
            "totalActiveReservations", reservations.size(),
            "walletsWithReservations", totalReservationsByWallet.size(),
            "totalReservationsByWallet", totalReservationsByWallet
        );
    }
}