package com.brokerx.wallet.dto.audit;

import java.util.Map;
import java.util.UUID;

public class CreateAuditLogRequest {
    public String entityType;
    public UUID entityId;
    public String action;
    public UUID performedBy;
    public String ipAddress;
    public Map<String, Object> details;
}