package com.brokerx.units.service;

import com.brokerx.dto.order.OrderRequest;
import com.brokerx.dto.order.OrderResponse;
import com.brokerx.entity.Account;
import com.brokerx.entity.Order;
import com.brokerx.entity.Wallet;
import com.brokerx.repository.OrderRepository;
import com.brokerx.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private WalletService walletService;
    @Mock
    private OrderValidationService validationService;
    @Mock
    private FundReservationService fundReservationService;

    @InjectMocks
    private OrderService orderService;

    private Account activeAccount;
    private Wallet wallet;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        UUID accountId = UUID.randomUUID();
        activeAccount = new Account();
        activeAccount.setAccountId(accountId);
        activeAccount.setStatus("ACTIVE");

        wallet = new Wallet();
        wallet.setWalletId(UUID.randomUUID());
        wallet.setAccount(activeAccount);
        wallet.setBalance(new BigDecimal("1000.00"));

        orderRequest = new OrderRequest();
        orderRequest.setClientOrderId("CLIENT_ORDER_001");
        orderRequest.setSymbol("AAPL");
        orderRequest.setSide("BUY");
        orderRequest.setOrderType("LIMIT");
        orderRequest.setQuantity(10);
        orderRequest.setPrice(new BigDecimal("150.00"));
        orderRequest.setTimeInForce("DAY");
    }

    @Test
    void placeOrder_Success() {
        // Given
        when(accountService.getAllAccounts()).thenReturn(Collections.singletonList(activeAccount));
        when(orderRepository.findByAccount_AccountIdAndClientOrderId(any(), anyString())).thenReturn(Optional.empty());
        when(walletService.getWalletByAccountId(activeAccount.getAccountId())).thenReturn(wallet);
        when(validationService.validateOrder(any(), any(), any(), any())).thenReturn(OrderValidationService.ValidationResult.success("Validation passed"));
        when(fundReservationService.calculateReservationAmount(any())).thenReturn(new BigDecimal("1500.00")); // 10 * 150
        when(fundReservationService.hasSufficientFunds(any(), any())).thenReturn(true);
       
       
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderToSave = invocation.getArgument(0);
            orderToSave.setOrderId(UUID.randomUUID());
            return orderToSave;
        });
        
        when(fundReservationService.reserveFunds(any(), any(), any())).thenReturn(true);

        // When
        OrderResponse response = orderService.placeOrder(orderRequest);

        // Then
        assertTrue(response.isSuccess());
        assertEquals("Order placed successfully", response.getMessage());
        assertNotNull(response.getOrderId());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(fundReservationService, times(1)).reserveFunds(any(), any(), any());
    }

    @Test
    void placeOrder_InsufficientFunds() {
        // Given
        when(accountService.getAllAccounts()).thenReturn(Collections.singletonList(activeAccount));
        when(walletService.getWalletByAccountId(activeAccount.getAccountId())).thenReturn(wallet);
        when(validationService.validateOrder(any(), any(), any(), any())).thenReturn(OrderValidationService.ValidationResult.success("Validation passed"));
        when(fundReservationService.calculateReservationAmount(any())).thenReturn(new BigDecimal("1500.00"));
        when(fundReservationService.hasSufficientFunds(wallet, new BigDecimal("1500.00"))).thenReturn(false);

        // When
        OrderResponse response = orderService.placeOrder(orderRequest);

        // Then
        assertFalse(response.isSuccess());
        assertEquals("Insufficient available funds after considering existing orders", response.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
    }
}