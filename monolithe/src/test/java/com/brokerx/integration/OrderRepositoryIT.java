package com.brokerx.integration;

import com.brokerx.entity.Account;
import com.brokerx.entity.Order;
import com.brokerx.entity.User;
import com.brokerx.repository.AccountRepository;
import com.brokerx.repository.OrderRepository;
import com.brokerx.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class OrderRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindOrder() {
        // Given - Create user and account first
        User user = new User();
        user.setEmail("order@test.com");
        user.setPhone("1234567890");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Order order = new Order();
        order.setAccount(savedAccount);
        order.setClientOrderId("TEST_ORDER_001");
        order.setSymbol("AAPL");
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setQuantity(100);
        order.setPrice(new BigDecimal("150.00"));
        order.setTimeInForce("DAY");
        order.setStatus("NEW");

        // When
        Order savedOrder = orderRepository.save(order);
        Optional<Order> foundOrder = orderRepository.findById(savedOrder.getOrderId());

        // Then
        assertTrue(foundOrder.isPresent());
        assertEquals("TEST_ORDER_001", foundOrder.get().getClientOrderId());
        assertEquals("AAPL", foundOrder.get().getSymbol());
        assertEquals("BUY", foundOrder.get().getSide());
        assertEquals(100, foundOrder.get().getQuantity());
        assertEquals(0, new BigDecimal("150.00").compareTo(foundOrder.get().getPrice()));
        assertEquals("NEW", foundOrder.get().getStatus());
    }

    @Test
    void testFindByAccountId() {
        // Given
        User user = new User();
        user.setEmail("multiorder@test.com");
        user.setPhone("0987654321");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Order order1 = new Order();
        order1.setAccount(savedAccount);
        order1.setClientOrderId("ORDER_1");
        order1.setSymbol("AAPL");
        order1.setSide("BUY");
        order1.setOrderType("LIMIT");
        order1.setQuantity(50);
        order1.setPrice(new BigDecimal("150.00"));
        order1.setTimeInForce("DAY");
        order1.setStatus("NEW");
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setAccount(savedAccount);
        order2.setClientOrderId("ORDER_2");
        order2.setSymbol("GOOGL");
        order2.setSide("SELL");
        order2.setOrderType("MARKET");
        order2.setQuantity(25);
        order2.setTimeInForce("IOC");
        order2.setStatus("WORKING");
        orderRepository.save(order2);

        // When
        List<Order> orders = orderRepository.findByAccount_AccountId(savedAccount.getAccountId());

        // Then
        assertEquals(2, orders.size());
        assertTrue(orders.stream().anyMatch(o -> "ORDER_1".equals(o.getClientOrderId())));
        assertTrue(orders.stream().anyMatch(o -> "ORDER_2".equals(o.getClientOrderId())));
    }

    @Test
    void testFindByAccountIdAndClientOrderId() {
        // Given
        User user = new User();
        user.setEmail("specific@test.com");
        user.setPhone("1112223333");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Order order = new Order();
        order.setAccount(savedAccount);
        order.setClientOrderId("SPECIFIC_ORDER");
        order.setSymbol("MSFT");
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setQuantity(75);
        order.setPrice(new BigDecimal("300.00"));
        order.setTimeInForce("DAY");
        order.setStatus("NEW");
        orderRepository.save(order);

        // When
        Optional<Order> foundOrder = orderRepository.findByAccount_AccountIdAndClientOrderId(
            savedAccount.getAccountId(), "SPECIFIC_ORDER");

        // Then
        assertTrue(foundOrder.isPresent());
        assertEquals("SPECIFIC_ORDER", foundOrder.get().getClientOrderId());
        assertEquals("MSFT", foundOrder.get().getSymbol());
    }

    @Test
    void testOrderStatusUpdate() {
        // Given
        User user = new User();
        user.setEmail("statusupdate@test.com");
        user.setPhone("4445556666");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Order order = new Order();
        order.setAccount(savedAccount);
        order.setClientOrderId("STATUS_ORDER");
        order.setSymbol("TSLA");
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setQuantity(30);
        order.setPrice(new BigDecimal("200.00"));
        order.setTimeInForce("DAY");
        order.setStatus("NEW");
        Order savedOrder = orderRepository.save(order);

        // When
        savedOrder.setStatus("WORKING");
        savedOrder.setFilledQuantity(15);
        orderRepository.save(savedOrder);

        // Then
        Optional<Order> updatedOrder = orderRepository.findById(savedOrder.getOrderId());
        assertTrue(updatedOrder.isPresent());
        assertEquals("WORKING", updatedOrder.get().getStatus());
        assertEquals(15, updatedOrder.get().getFilledQuantity());
        assertEquals(15, updatedOrder.get().getRemainingQuantity()); // 30 - 15
        assertNotNull(updatedOrder.get().getUpdatedAt());
    }
}