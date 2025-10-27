package com.brokerx.dto.audit;

import com.brokerx.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.UUID;


public class AuditLogDto {
    private UUID logId;
    private String entityType;
    private UUID entityId;
    private String action;
    private UUID performedBy;
    private LocalDateTime performedAt;
    private String ipAddress;
    private String oldValues;
    private String newValues;

    public AuditLogDto(AuditLog auditLog) {
        this.logId = auditLog.getLogId();
        this.entityType = auditLog.getEntityType();
        this.entityId = auditLog.getEntityId();
        this.action = auditLog.getAction();
        this.performedBy = auditLog.getPerformedBy();
        this.performedAt = auditLog.getPerformedAt();
        this.ipAddress = auditLog.getIpAddress();
        this.oldValues = auditLog.getOldValues();
        this.newValues = auditLog.getNewValues();
    }

    public UUID getLogId() { return logId; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getAction() { return action; }
    public UUID getPerformedBy() { return performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getOldValues() { return oldValues; }
    public String getNewValues() { return newValues; }
}