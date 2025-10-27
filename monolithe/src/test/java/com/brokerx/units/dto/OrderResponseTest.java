package com.brokerx.units.dto;

import com.brokerx.dto.order.OrderResponse;
import com.brokerx.entity.Account;
import com.brokerx.entity.Order;
import org.junit.jupiter.api.Test;


import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OrderResponseTest {

    @Test
    void testOrderResponse_SuccessConstructor() {
        Account account = new Account();
        account.setAccountId(UUID.randomUUID());
        
        Order order = new Order();
        order.setOrderId(UUID.randomUUID());
        order.setAccount(account);
        order.setClientOrderId("TEST_123");
        order.setSymbol("AAPL");
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setQuantity(100);
        order.setPrice(new BigDecimal("150.00"));
        order.setTimeInForce("DAY");
        order.setStatus("NEW");
        order.setFilledQuantity(0);

        OrderResponse response = new OrderResponse(order, "Success message");

        assertTrue(response.isSuccess());
        assertEquals("Success message", response.getMessage());
        assertEquals(order.getOrderId(), response.getOrderId());
        assertEquals(account.getAccountId(), response.getAccountId());
        assertEquals("TEST_123", response.getClientOrderId());
        assertEquals("AAPL", response.getSymbol());
        assertEquals("BUY", response.getSide());
        assertEquals("LIMIT", response.getOrderType());
        assertEquals(100, response.getQuantity());
        assertEquals(0, new BigDecimal("150.00").compareTo(response.getPrice()));
        assertEquals("DAY", response.getTimeInForce());
        assertEquals("NEW", response.getStatus());
        assertEquals(0, response.getFilledQuantity());
    }

    @Test
    void testOrderResponse_FailureConstructor() {
        OrderResponse response = new OrderResponse("Error message", false);

        assertFalse(response.isSuccess());
        assertEquals("Error message", response.getMessage());
        assertNull(response.getOrderId());
        assertNull(response.getAccountId());
        assertNull(response.getClientOrderId());
    }

    @Test
    void testOrderResponse_WithNullAccount() {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID());
        order.setClientOrderId("TEST_NO_ACCOUNT");
        order.setSymbol("AAPL");
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setQuantity(50);
        order.setPrice(new BigDecimal("100.00"));
        order.setTimeInForce("DAY");
        order.setStatus("NEW");
        order.setFilledQuantity(0);

        OrderResponse response = new OrderResponse(order, "Order without account");

        assertTrue(response.isSuccess());
        assertNull(response.getAccountId()); // Account ID devrait être null
        assertEquals("TEST_NO_ACCOUNT", response.getClientOrderId());
    }

    @Test
    void testOrderResponse_PartiallyFilledOrder() {
        Account account = new Account();
        account.setAccountId(UUID.randomUUID());
        
        Order order = new Order();
        order.setOrderId(UUID.randomUUID());
        order.setAccount(account);
        order.setClientOrderId("PARTIAL_FILL");
        order.setSymbol("GOOGL");
        order.setSide("SELL");
        order.setOrderType("MARKET");
        order.setQuantity(200);
        order.setTimeInForce("IOC");
        order.setStatus("WORKING");
        order.setFilledQuantity(75); 

        OrderResponse response = new OrderResponse(order, "Partially filled");

        assertTrue(response.isSuccess());
        assertEquals(75, response.getFilledQuantity());
        assertEquals(125, response.getRemainingQuantity()); 
        assertEquals("WORKING", response.getStatus());
    }
}