package com.brokerx.units.dto;

import org.junit.jupiter.api.Test;
import com.brokerx.dto.order.OrderRequest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderRequestTest {

    @Test
    void testOrderRequestSettersAndGetters() {
        OrderRequest request = new OrderRequest();
        
        request.setClientOrderId("TEST_123");
        request.setSymbol("AAPL");
        request.setSide("BUY");
        request.setOrderType("LIMIT");
        request.setQuantity(100);
        request.setPrice(new BigDecimal("150.00"));
        request.setTimeInForce("DAY");
        
        assertEquals("TEST_123", request.getClientOrderId());
        assertEquals("AAPL", request.getSymbol());
        assertEquals("BUY", request.getSide());
        assertEquals("LIMIT", request.getOrderType());
        assertEquals(100, request.getQuantity());
        assertEquals(0, new BigDecimal("150.00").compareTo(request.getPrice()));
        assertEquals("DAY", request.getTimeInForce());
    }

    @Test
    void testOrderRequestToString() {
        OrderRequest request = new OrderRequest();
        request.setClientOrderId("TEST_123");
        request.setSymbol("AAPL");
        request.setSide("BUY");
        
        String toString = request.toString();
        
        assertTrue(toString.contains("TEST_123"));
        assertTrue(toString.contains("AAPL"));
        assertTrue(toString.contains("BUY"));
    }
}