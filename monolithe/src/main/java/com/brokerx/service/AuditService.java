package com.brokerx.service;

import com.brokerx.entity.AuditLog;
import com.brokerx.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Enregistre une action dans le journal d'audit
     */
    public void logAction(String entityType, UUID entityId, String action, UUID performedBy, 
                         String ipAddress, Object newValues) {
        try {
            AuditLog log = new AuditLog(entityType, entityId, action, performedBy);
            log.setIpAddress(ipAddress);
            
            if (newValues != null) {
                log.setNewValues(objectMapper.writeValueAsString(newValues));
            }
            
            auditLogRepository.save(log);
            
            logger.info("✅ Audit logged: entityType={}, entityId={}, action={}, performedBy={}", 
                       entityType, entityId, action, performedBy);
        } catch (Exception e) {
            logger.error("❌ Failed to log audit: entityType={}, entityId={}, action={}", 
                        entityType, entityId, action, e);
        }
    }

    /**
     * Enregistre une action avec anciennes et nouvelles valeurs
     */
    public void logUpdate(String entityType, UUID entityId, UUID performedBy, 
                         String ipAddress, Object oldValues, Object newValues) {
        try {
            AuditLog log = new AuditLog(entityType, entityId, "UPDATE", performedBy);
            log.setIpAddress(ipAddress);
            
            if (oldValues != null) {
                log.setOldValues(objectMapper.writeValueAsString(oldValues));
            }
            if (newValues != null) {
                log.setNewValues(objectMapper.writeValueAsString(newValues));
            }
            
            auditLogRepository.save(log);
            
            logger.info("✅ Audit UPDATE logged: entityType={}, entityId={}, performedBy={}", 
                       entityType, entityId, performedBy);
        } catch (Exception e) {
            logger.error("❌ Failed to log audit update: entityType={}, entityId={}", 
                        entityType, entityId, e);
        }
    }

    /**
     * Enregistre une connexion réussie
     */
    public void logLogin(UUID userId, String ipAddress, String userAgent) {
        logAction("USER", userId, "LOGIN", userId, ipAddress, 
                 new LoginInfo(userAgent, "SUCCESS"));
    }

    /**
     * Enregistre une tentative MFA
     */
    public void logMfaAttempt(UUID userId, String ipAddress, boolean success) {
        String action = success ? "MFA_SUCCESS" : "MFA_FAILURE";
        logAction("USER", userId, action, userId, ipAddress, 
                 new MfaInfo(success));
    }

    /**
     * Enregistre la création d'un ordre
     */
    public void logOrderCreation(UUID orderId, UUID userId, String symbol, String side, 
                                Integer quantity, String ipAddress) {
        logAction("ORDER", orderId, "CREATE", userId, ipAddress,
                 new OrderInfo(symbol, side, quantity));
    }

    /**
     * Enregistre la modification d'un ordre
     */
    public void logOrderModification(UUID orderId, UUID userId, String ipAddress,
                                    Object oldValues, Object newValues) {
        logUpdate("ORDER", orderId, userId, ipAddress, oldValues, newValues);
    }

    /**
     * Enregistre l'annulation d'un ordre
     */
    public void logOrderCancellation(UUID orderId, UUID userId, String ipAddress) {
        logAction("ORDER", orderId, "DELETE", userId, ipAddress, 
                 new CancellationInfo("User requested cancellation"));
    }

    // Classes internes pour structure des valeurs JSON
    private static class LoginInfo {
        public String userAgent;
        public String status;
        
        public LoginInfo(String userAgent, String status) {
            this.userAgent = userAgent;
            this.status = status;
        }
    }

    private static class MfaInfo {
        public boolean success;
        
        public MfaInfo(boolean success) {
            this.success = success;
        }
    }

    private static class OrderInfo {
        public String symbol;
        public String side;
        public Integer quantity;
        
        public OrderInfo(String symbol, String side, Integer quantity) {
            this.symbol = symbol;
            this.side = side;
            this.quantity = quantity;
        }
    }

    private static class CancellationInfo {
        public String reason;
        
        public CancellationInfo(String reason) {
            this.reason = reason;
        }
    }
}