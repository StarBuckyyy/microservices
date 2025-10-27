package com.brokerx.service;

import com.brokerx.dto.audit.AuditLogDto;
import com.brokerx.entity.AuditLog;
import com.brokerx.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }
    
    public void logAction(String entityType, UUID entityId, String action, UUID performedBy, String ipAddress, Map<String, Object> details) {
        try {
            AuditLog log = new AuditLog();
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setAction(action);
            log.setPerformedBy(performedBy);
            log.setIpAddress(ipAddress);
            
            if (details != null && !details.isEmpty()) {
                log.setNewValues(objectMapper.writeValueAsString(details));
            }
            
            auditLogRepository.save(log);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing audit details", e);
        }
    }
    
    public List<AuditLogDto> getAllLogs() {
        return auditLogRepository.findAll()
                .stream()
                .map(AuditLogDto::new)
                .collect(Collectors.toList());
    }

    public List<AuditLogDto> getRecentLogs(int limit) {
        return auditLogRepository.findTopNByOrderByPerformedAtDesc(limit)
                .stream()
                .map(AuditLogDto::new)
                .collect(Collectors.toList());
    }

    public List<AuditLogDto> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action)
                .stream()
                .map(AuditLogDto::new)
                .collect(Collectors.toList());
    }

    public List<AuditLogDto> getLogsByEntityType(String entityType) {
        return auditLogRepository.findByEntityType(entityType)
                .stream()
                .map(AuditLogDto::new)
                .collect(Collectors.toList());
    }
    
    // Logique pour logLogin, logMfaAttempt, etc. peut être ajoutée ici si nécessaire
    public void logLogin(UUID userId, String ipAddress, String userAgent) {
        logAction("USER", userId, "LOGIN", userId, ipAddress, 
                 Map.of("userAgent", userAgent != null ? userAgent : "unknown"));
    }

    public void logMfaAttempt(UUID userId, String ipAddress, boolean success) {
        String action = success ? "MFA_SUCCESS" : "MFA_FAILURE";
        logAction("USER", userId, action, userId, ipAddress, Map.of("success", success));
    }
}