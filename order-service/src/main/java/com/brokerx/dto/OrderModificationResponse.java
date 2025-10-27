package com.brokerx.dto.order;

import java.util.UUID;

public class OrderModificationResponse {
    private boolean success;
    private String message;
    private UUID orderId;
    private String action; // "CANCELLED" ou "MODIFIED"
    private OrderResponse order;

    public OrderModificationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public OrderModificationResponse(boolean success, String message, UUID orderId, String action) {
        this.success = success;
        this.message = message;
        this.orderId = orderId;
        this.action = action;
    }

    public OrderModificationResponse(boolean success, String message, OrderResponse order, String action) {
        this.success = success;
        this.message = message;
        this.order = order;
        this.action = action;
        if (order != null) {
            this.orderId = order.getOrderId();
        }
    }

    // Getters et setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public OrderResponse getOrder() { return order; }
    public void setOrder(OrderResponse order) { this.order = order; }
}