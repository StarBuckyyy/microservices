package com.brokerx.service;

import com.brokerx.dto.audit.CreateAuditLogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final RestTemplate restTemplate;
    
    private static final String ACCOUNT_SERVICE_URL = "http://account-service:8081";

    public AuditService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void logAction(String entityType, UUID entityId, String action, 
                         UUID performedBy, String ipAddress, Map<String, Object> details) {
        try {
            CreateAuditLogRequest logRequest = new CreateAuditLogRequest();
            logRequest.entityType = entityType;
            logRequest.entityId = entityId;
            logRequest.action = action;
            logRequest.performedBy = performedBy;
            logRequest.ipAddress = ipAddress;
            logRequest.details = details;

            restTemplate.postForObject(
                ACCOUNT_SERVICE_URL + "/audit", 
                logRequest, 
                Void.class
            );
            
            logger.debug("Audit log sent: {} {} {}", entityType, entityId, action);
            
        } catch (Exception e) {
            logger.error("Failed to send audit log: {}", e.getMessage());
        }
    }
}