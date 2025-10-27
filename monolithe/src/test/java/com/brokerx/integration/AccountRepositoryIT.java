package com.brokerx.integration;

import com.brokerx.entity.Account;
import com.brokerx.entity.User;
import com.brokerx.repository.AccountRepository;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class AccountRepositoryIT extends BaseIntegrationTest {


    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindAccount() {
        // Given
        User user = new User();
        user.setEmail("test@integration.com");
        user.setPhone("1234567890");
        user.setHashedPassword("hashedpass");
        user.setFirstName("Integration");
        user.setLastName("Test");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");

        // When
        Account savedAccount = accountRepository.save(account);
        Optional<Account> foundAccount = accountRepository.findById(savedAccount.getAccountId());

        // Then
        assertTrue(foundAccount.isPresent());
        assertEquals("ACTIVE", foundAccount.get().getStatus());
        assertEquals(savedUser.getUserId(), foundAccount.get().getUser().getUserId());
    }

    @Test
    void testFindByUserId() {
        // Given
        User user = new User();
        user.setEmail("findbyuser@test.com");
        user.setPhone("0987654321");
        user.setHashedPassword("pass");
        user.setFirstName("Find");
        user.setLastName("User");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("PENDING");
        accountRepository.save(account);

        // When
        Optional<Account> foundAccount = accountRepository.findByUser_UserId(savedUser.getUserId());

        // Then
        assertTrue(foundAccount.isPresent());
        assertEquals("PENDING", foundAccount.get().getStatus());
        assertEquals(savedUser.getUserId(), foundAccount.get().getUser().getUserId());
    }

    @Test
    void testFindByUserId_NotFound() {
        // When
        Optional<Account> foundAccount = accountRepository.findByUser_UserId(UUID.randomUUID());

        // Then
        assertFalse(foundAccount.isPresent());
    }

    @Test
    void testAccountStatusUpdate() {
        // Given
        User user = new User();
        user.setEmail("update@test.com");
        user.setPhone("1112223333");
        user.setHashedPassword("pass");
        user.setFirstName("Update");
        user.setLastName("Test");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("PENDING");
        Account savedAccount = accountRepository.save(account);

        // When
        savedAccount.setStatus("ACTIVE");
        accountRepository.save(savedAccount);

        // Then
        Optional<Account> updatedAccount = accountRepository.findById(savedAccount.getAccountId());
        assertTrue(updatedAccount.isPresent());
        assertEquals("ACTIVE", updatedAccount.get().getStatus());
        assertNotNull(updatedAccount.get().getUpdatedAt());
    }
}