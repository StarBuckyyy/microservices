package com.brokerx.service;

import com.brokerx.entity.Order;
import com.brokerx.entity.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion des réservations de fonds pour les ordres
 * Maintient en mémoire les fonds réservés par ordre
 */
@Service
public class FundReservationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FundReservationService.class);
    
    // Map: OrderId -> Montant réservé
    private final Map<UUID, BigDecimal> reservations = new ConcurrentHashMap<>();
    
    // Map: WalletId -> Total des réservations
    private final Map<UUID, BigDecimal> totalReservationsByWallet = new ConcurrentHashMap<>();
    
    /**
     * Réserve des fonds pour un ordre
     */
    public boolean reserveFunds(UUID orderId, UUID walletId, BigDecimal amount) {
        logger.info("Attempting to reserve funds: orderId={}, walletId={}, amount={}", 
                   orderId, walletId, amount);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid reservation amount: {}", amount);
            return false;
        }
        
        // Vérifier si l'ordre a déjà une réservation
        if (reservations.containsKey(orderId)) {
            logger.warn("Order already has funds reserved: orderId={}", orderId);
            return false;
        }
        
        // Enregistrer la réservation
        reservations.put(orderId, amount);
        
        // Mettre à jour le total des réservations pour ce wallet
        totalReservationsByWallet.merge(walletId, amount, BigDecimal::add);
        
        logger.info("Funds reserved successfully: orderId={}, walletId={}, amount={}, totalReserved={}", 
                   orderId, walletId, amount, totalReservationsByWallet.get(walletId));
        
        return true;
    }
    
    /**
     * Libère les fonds réservés pour un ordre
     */
    public void releaseFunds(UUID orderId, UUID walletId) {
        BigDecimal reservedAmount = reservations.remove(orderId);
        
        if (reservedAmount != null) {
            totalReservationsByWallet.merge(walletId, reservedAmount.negate(), BigDecimal::add);
            
            // Nettoyer si le total est 0
            if (totalReservationsByWallet.get(walletId).compareTo(BigDecimal.ZERO) == 0) {
                totalReservationsByWallet.remove(walletId);
            }
            
            logger.info("Funds released: orderId={}, walletId={}, amount={}", 
                       orderId, walletId, reservedAmount);
        } else {
            logger.warn("No reservation found for order: orderId={}", orderId);
        }
    }
    
    /**
     * Calcule le solde disponible (solde réel - réservations)
     */
    public BigDecimal getAvailableBalance(Wallet wallet) {
        BigDecimal totalReserved = totalReservationsByWallet.getOrDefault(
            wallet.getWalletId(), BigDecimal.ZERO);
        
        BigDecimal available = wallet.getBalance().subtract(totalReserved);
        
        logger.debug("Available balance calculation: walletId={}, balance={}, reserved={}, available={}", 
                    wallet.getWalletId(), wallet.getBalance(), totalReserved, available);
        
        return available;
    }
    
    /**
     * Retourne le montant total réservé pour un wallet
     */
    public BigDecimal getTotalReserved(UUID walletId) {
        return totalReservationsByWallet.getOrDefault(walletId, BigDecimal.ZERO);
    }
    
    /**
     * Retourne le montant réservé pour un ordre spécifique
     */
    public BigDecimal getReservedAmount(UUID orderId) {
        return reservations.getOrDefault(orderId, BigDecimal.ZERO);
    }
    
    /**
     * Vérifie si un wallet a suffisamment de fonds disponibles
     */
    public boolean hasSufficientFunds(Wallet wallet, BigDecimal requiredAmount) {
        BigDecimal available = getAvailableBalance(wallet);
        boolean sufficient = available.compareTo(requiredAmount) >= 0;
        
        logger.debug("Funds sufficiency check: walletId={}, required={}, available={}, sufficient={}", 
                    wallet.getWalletId(), requiredAmount, available, sufficient);
        
        return sufficient;
    }
    
    /**
     * Calcule le montant à réserver pour un ordre
     */
    public BigDecimal calculateReservationAmount(Order order) {
        if (!"BUY".equals(order.getSide())) {
            return BigDecimal.ZERO; // Pas de réservation pour les ordres de vente
        }
        
        if ("MARKET".equals(order.getOrderType())) {
            // Pour les ordres MARKET, on estime le coût maximal
            // En production, cela viendrait du market data service
            return new BigDecimal(order.getQuantity()).multiply(new BigDecimal("100.00"));
        } else if ("LIMIT".equals(order.getOrderType())) {
            // Pour les ordres LIMIT, on calcule le coût exact
            return new BigDecimal(order.getQuantity()).multiply(order.getPrice());
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Méthode utilitaire pour obtenir des statistiques (debug/monitoring)
     */
    public Map<String, Object> getReservationStats() {
        return Map.of(
            "totalActiveReservations", reservations.size(),
            "walletsWithReservations", totalReservationsByWallet.size(),
            "totalReservationsByWallet", totalReservationsByWallet
        );
    }
}