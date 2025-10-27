package com.brokerx.controller;

import com.brokerx.dto.audit.AuditLogDto;
import com.brokerx.dto.audit.CreateAuditLogRequest;
import com.brokerx.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // Endpoint pour la création de logs par d'autres services
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createAuditLog(@RequestBody CreateAuditLogRequest request) {
        auditService.logAction(
            request.entityType,
            request.entityId,
            request.action,
            request.performedBy,
            request.ipAddress,
            request.details
        );
    }

    // Endpoint pour LIRE tous les logs
    @GetMapping
    public ResponseEntity<List<AuditLogDto>> getAllAuditLogs() {
        return ResponseEntity.ok(auditService.getAllLogs());
    }

    // Endpoint pour LIRE les logs récents
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLogDto>> getRecentLogs(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(auditService.getRecentLogs(limit));
    }

    // Endpoint pour LIRE par action
    @GetMapping("/by-action/{action}")
    public ResponseEntity<List<AuditLogDto>> getLogsByAction(@PathVariable String action) {
        return ResponseEntity.ok(auditService.getLogsByAction(action));
    }

    // Endpoint pour LIRE par type d'entité
    @GetMapping("/by-entity-type/{entityType}")
    public ResponseEntity<List<AuditLogDto>> getLogsByEntityType(@PathVariable String entityType) {
        return ResponseEntity.ok(auditService.getLogsByEntityType(entityType));
    }
}