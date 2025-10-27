package com.brokerx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"account_id", "client_order_id"})
       })
@Check(constraints = "quantity > 0 AND filled_quantity >= 0 AND remaining_quantity >= 0 AND filled_quantity + remaining_quantity = quantity AND side IN ('BUY', 'SELL') AND order_type IN ('MARKET', 'LIMIT') AND time_in_force IN ('DAY', 'IOC', 'FOK') AND status IN ('NEW', 'WORKING', 'FILLED', 'CANCELLED', 'REJECTED')")
public class Order {

    @Id
    @GeneratedValue
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    // ✅ Juste l'ID, pas de relation JPA
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "client_order_id", nullable = false, length = 50)
    private String clientOrderId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String side;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "time_in_force", nullable = false, length = 10)
    private String timeInForce;

    @Column(nullable = false, length = 20)
    private String status = "NEW";

    @Column(name = "filled_quantity", nullable = false)
    private Integer filledQuantity = 0;

    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public Order() {}

    // Getters/Setters
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) {
        if (!side.equals("BUY") && !side.equals("SELL")) {
            throw new IllegalArgumentException("Invalid side");
        }
        this.side = side;
    }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) {
        if (!orderType.equals("MARKET") && !orderType.equals("LIMIT")) {
            throw new IllegalArgumentException("Invalid order type");
        }
        this.orderType = orderType;
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }
        this.quantity = quantity;
        this.remainingQuantity = quantity;
    }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getTimeInForce() { return timeInForce; }
    public void setTimeInForce(String timeInForce) {
        if (!timeInForce.equals("DAY") && !timeInForce.equals("IOC") && !timeInForce.equals("FOK")) {
            throw new IllegalArgumentException("Invalid timeInForce");
        }
        this.timeInForce = timeInForce;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        if (!status.equals("NEW") && !status.equals("WORKING") && !status.equals("FILLED") &&
            !status.equals("CANCELLED") && !status.equals("REJECTED")) {
            throw new IllegalArgumentException("Invalid status");
        }
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(Integer filledQuantity) {
        if (filledQuantity < 0) {
            throw new IllegalArgumentException("Filled quantity cannot be negative");
        }
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = this.quantity - filledQuantity;
    }

    public Integer getRemainingQuantity() { return remainingQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Integer getVersion() { return version; }
}