package com.brokerx.repository;

import com.brokerx.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    List<AuditLog> findByAction(String action);
    
    List<AuditLog> findByEntityType(String entityType);
    
    @Query(value = "SELECT * FROM audit_logs ORDER BY performed_at DESC LIMIT :limit", nativeQuery = true)
    List<AuditLog> findTopNByOrderByPerformedAtDesc(@Param("limit") int limit);
}