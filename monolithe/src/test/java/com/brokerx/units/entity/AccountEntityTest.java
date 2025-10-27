package com.brokerx.units.entity;

import org.junit.jupiter.api.Test;
import com.brokerx.entity.Account;

import static org.junit.jupiter.api.Assertions.*;

class AccountEntityTest {

    @Test
    void testAccountStatus_ValidStatus() {
        Account account = new Account();
        
        assertDoesNotThrow(() -> account.setStatus("PENDING"));
        assertDoesNotThrow(() -> account.setStatus("ACTIVE"));
        assertDoesNotThrow(() -> account.setStatus("REJECTED"));
    }

    @Test
    void testAccountStatus_InvalidStatus() {
        Account account = new Account();
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            account.setStatus("INVALID_STATUS");
        });
        
        assertTrue(exception.getMessage().contains("Invalid status"));
    }

    @Test
    void testAccountStatus_UpdatesTimestamp() {
        Account account = new Account();
        account.setStatus("PENDING");
        assertNotNull(account.getUpdatedAt());
    }
}