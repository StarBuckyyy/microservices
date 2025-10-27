package com.brokerx.controller;

import com.brokerx.entity.AuditLog;
import com.brokerx.repository.AuditLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * ✅ FONCTIONNE - Récupérer tous les logs d'audit
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    /**
     * 🔧 FIX 1 : Récupérer les logs par type d'entité
     * AVANT : @GetMapping("/entity-type/{entityType}")
     * PROBLÈME : Conflit avec le wildcard "/**"
     */
    @GetMapping("/by-entity-type/{entityType}")
    public ResponseEntity<List<AuditLog>> getByEntityType(@PathVariable String entityType) {
        return ResponseEntity.ok(
            auditLogRepository.findByEntityTypeOrderByPerformedAtDesc(entityType)
        );
    }

    /**
     * 🔧 FIX 2 : Récupérer les logs pour une entité spécifique
     * Changement de route pour éviter les conflits
     */
    @GetMapping("/by-entity/{entityType}/{entityId}")
    public ResponseEntity<List<AuditLog>> getByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        return ResponseEntity.ok(
            auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(entityType, entityId)
        );
    }

    /**
     * 🔧 FIX 3 : Récupérer les logs par utilisateur
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<AuditLog>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(
            auditLogRepository.findByPerformedByOrderByPerformedAtDesc(userId)
        );
    }

    /**
     * 🔧 FIX 4 : Récupérer les logs par action
     */
    @GetMapping("/by-action/{action}")
    public ResponseEntity<List<AuditLog>> getByAction(@PathVariable String action) {
        return ResponseEntity.ok(
            auditLogRepository.findByActionOrderByPerformedAtDesc(action)
        );
    }

    /**
     * 🔧 FIX 5 : Récupérer les logs dans une période
     * IMPORTANT : Utiliser le format ISO 8601 pour les dates
     * Exemple : ?start=2025-10-24T00:00:00&end=2025-10-24T23:59:59
     */
    @GetMapping("/by-period")
    public ResponseEntity<List<AuditLog>> getByPeriod(
            @RequestParam String start,
            @RequestParam String end) {
        try {
            LocalDateTime startDate = LocalDateTime.parse(start);
            LocalDateTime endDate = LocalDateTime.parse(end);
            return ResponseEntity.ok(
                auditLogRepository.findByPeriod(startDate, endDate)
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 🆕 NOUVEAU : Endpoint simple pour tester
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getAuditCount() {
        return ResponseEntity.ok(auditLogRepository.count());
    }

    /**
     * 🆕 NOUVEAU : Derniers logs (les plus récents)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AuditLog>> getRecentLogs(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
            auditLogRepository.findAll().stream()
                .sorted((a, b) -> b.getPerformedAt().compareTo(a.getPerformedAt()))
                .limit(limit)
                .toList()
        );
    }
}