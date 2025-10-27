package com.brokerx.repository;

import com.brokerx.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Rechercher par type d'entité
    List<AuditLog> findByEntityTypeOrderByPerformedAtDesc(String entityType);
    
    // Rechercher par entité spécifique
    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, UUID entityId);
    
    // Rechercher par utilisateur
    List<AuditLog> findByPerformedByOrderByPerformedAtDesc(UUID performedBy);
    
    // Rechercher par action
    List<AuditLog> findByActionOrderByPerformedAtDesc(String action);
    
    // Rechercher dans une période
    @Query("SELECT a FROM AuditLog a WHERE a.performedAt BETWEEN :start AND :end ORDER BY a.performedAt DESC")
    List<AuditLog> findByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Recherche combinée
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.action = :action ORDER BY a.performedAt DESC")
    List<AuditLog> findByEntityAndAction(@Param("entityType") String entityType, 
                                         @Param("entityId") UUID entityId, 
                                         @Param("action") String action);
}