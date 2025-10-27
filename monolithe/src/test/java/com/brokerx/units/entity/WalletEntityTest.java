package com.brokerx.units.entity;

import org.junit.jupiter.api.Test;
import com.brokerx.entity.Wallet;   

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WalletEntityTest {

    @Test
    void testWalletBalance_PositiveBalance() {
        Wallet wallet = new Wallet();
        
        assertDoesNotThrow(() -> wallet.setBalance(new BigDecimal("1000.00")));
        assertEquals(0, new BigDecimal("1000.00").compareTo(wallet.getBalance()));
    }

    @Test
    void testWalletBalance_NegativeBalance() {
        Wallet wallet = new Wallet();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            wallet.setBalance(new BigDecimal("-100.00"));
        });
        
        assertTrue(exception.getMessage().contains("Balance cannot be negative"));
    }

    @Test
    void testWalletCurrency_OnlyUSDAllowed() {
        Wallet wallet = new Wallet();
        
        assertDoesNotThrow(() -> wallet.setCurrency("USD"));
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            wallet.setCurrency("EUR");
        });
        
        assertTrue(exception.getMessage().contains("Currency must be USD"));
    }

    @Test
    void testWalletBalance_UpdatesTimestamp() {
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.TEN);
        
        assertNotNull(wallet.getUpdatedAt());
    }
}