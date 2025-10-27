package com.brokerx.dto.order;

import java.util.UUID;

public class CancelOrderRequest {
    private UUID orderId;
    private String clientOrderId;

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
}