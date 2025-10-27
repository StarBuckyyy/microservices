package com.brokerx.dto.order;

import com.brokerx.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class OrderResponse {
    private UUID orderId;
    private UUID accountId;
    private String clientOrderId;
    private String symbol;
    private String side;
    private String orderType;
    private Integer quantity;
    private BigDecimal price;
    private String timeInForce;
    private String status;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private LocalDateTime createdAt;
    private String message;
    private boolean success;
    
    public OrderResponse() {}

    public OrderResponse(Order order, String message) {
        if (order != null) {
            this.orderId = order.getOrderId();
            this.accountId = order.getAccountId();
            this.clientOrderId = order.getClientOrderId();
            this.symbol = order.getSymbol();
            this.side = order.getSide();
            this.orderType = order.getOrderType();
            this.quantity = order.getQuantity();
            this.price = order.getPrice();
            this.timeInForce = order.getTimeInForce();
            this.status = order.getStatus();
            this.filledQuantity = order.getFilledQuantity();
            this.remainingQuantity = order.getRemainingQuantity();
            this.createdAt = order.getCreatedAt();
        }
        this.message = message;
        this.success = true;
    }

    public OrderResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    // Getters/Setters
    public UUID getOrderId() { return orderId; }
    public UUID getAccountId() { return accountId; }
    public String getClientOrderId() { return clientOrderId; }
    public String getSymbol() { return symbol; }
    public String getSide() { return side; }
    public String getOrderType() { return orderType; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
    public String getTimeInForce() { return timeInForce; }
    public String getStatus() { return status; }
    public Integer getFilledQuantity() { return filledQuantity; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
}