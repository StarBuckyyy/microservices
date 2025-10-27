package com.brokerx.dto.order;

import java.math.BigDecimal;

public class OrderRequest {
    private String clientOrderId;
    private String symbol;
    private String side; // BUY, SELL
    private String orderType; // MARKET, LIMIT
    private Integer quantity;
    private BigDecimal price; // Optionnel pour MARKET
    private String timeInForce; // DAY, IOC, FOK

    // Constructeur par défaut
    public OrderRequest() {}

    // Getters et Setters (accountId retiré)
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getTimeInForce() { return timeInForce; }
    public void setTimeInForce(String timeInForce) { this.timeInForce = timeInForce; }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "clientOrderId='" + clientOrderId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side='" + side + '\'' +
                ", orderType='" + orderType + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", timeInForce='" + timeInForce + '\'' +
                '}';
    }
}