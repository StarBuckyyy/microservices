package com.brokerx.units.entity;

import org.junit.jupiter.api.Test;
import com.brokerx.entity.Order;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderEntityTest {

    @Test
    void testOrderCreation_ValidData() {
        Order order = new Order();
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setQuantity(100);
        order.setPrice(new BigDecimal("150.00"));
        order.setTimeInForce("DAY");
        
        assertEquals("BUY", order.getSide());
        assertEquals(100, order.getQuantity());
        assertEquals(100, order.getRemainingQuantity());
    }

    @Test
    void testOrderCreation_InvalidSide() {
        Order order = new Order();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            order.setSide("INVALID_SIDE");
        });
        
        assertTrue(exception.getMessage().contains("Invalid side"));
    }

    @Test
    void testOrderCreation_InvalidQuantity() {
        Order order = new Order();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            order.setQuantity(0);
        });
        
        assertTrue(exception.getMessage().contains("Quantity must be > 0"));
    }

    @Test
    void testFilledQuantityCalculation() {
        Order order = new Order();
        order.setQuantity(100);
        order.setFilledQuantity(30);
        
        assertEquals(30, order.getFilledQuantity());
        assertEquals(70, order.getRemainingQuantity());
    }

    @Test
    void testFilledQuantity_InvalidValue() {
        Order order = new Order();
        order.setQuantity(100);
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            order.setFilledQuantity(-10);
        });
        
        assertTrue(exception.getMessage().contains("Filled quantity cannot be negative"));
    }
}