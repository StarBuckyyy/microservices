package com.brokerx.integration;

import com.brokerx.entity.Account;
import com.brokerx.entity.User;
import com.brokerx.entity.Wallet;
import com.brokerx.repository.AccountRepository;
import com.brokerx.repository.UserRepository;
import com.brokerx.repository.WalletRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class WalletRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindWallet() {
        // Given
        User user = new User();
        user.setEmail("wallet@test.com");
        user.setPhone("1234567890");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Wallet wallet = new Wallet();
        wallet.setAccount(savedAccount);
        wallet.setBalance(new BigDecimal("1000.50"));
        wallet.setCurrency("USD");

        // When
        Wallet savedWallet = walletRepository.save(wallet);
        Optional<Wallet> foundWallet = walletRepository.findById(savedWallet.getWalletId());

        // Then
        assertTrue(foundWallet.isPresent());
        assertEquals(0, new BigDecimal("1000.50").compareTo(foundWallet.get().getBalance()));
        assertEquals("USD", foundWallet.get().getCurrency());
        assertEquals(savedAccount.getAccountId(), foundWallet.get().getAccount().getAccountId());
    }

    @Test
    void testFindByAccountId() {
        // Given
        User user = new User();
        user.setEmail("findwallet@test.com");
        user.setPhone("0987654321");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Wallet wallet = new Wallet();
        wallet.setAccount(savedAccount);
        wallet.setBalance(new BigDecimal("500.00"));
        walletRepository.save(wallet);

        // When
        Optional<Wallet> foundWallet = walletRepository.findByAccount_AccountId(savedAccount.getAccountId());

        // Then
        assertTrue(foundWallet.isPresent());
        assertEquals(0, new BigDecimal("500.00").compareTo(foundWallet.get().getBalance()));
    }

    @Test
    void testWalletBalanceUpdate() {
        // Given
        User user = new User();
        user.setEmail("updatebalance@test.com");
        user.setPhone("1112223333");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Wallet wallet = new Wallet();
        wallet.setAccount(savedAccount);
        wallet.setBalance(new BigDecimal("100.00"));
        Wallet savedWallet = walletRepository.save(wallet);

        // When
        savedWallet.setBalance(new BigDecimal("250.75"));
        walletRepository.save(savedWallet);

        // Then
        Optional<Wallet> updatedWallet = walletRepository.findById(savedWallet.getWalletId());
        assertTrue(updatedWallet.isPresent());
        assertEquals(0, new BigDecimal("250.75").compareTo(updatedWallet.get().getBalance()));
        assertNotNull(updatedWallet.get().getUpdatedAt());
    }

    @Test
    void testWalletBalanceCannotBeNegative() {
        // Given
        User user = new User();
        user.setEmail("negative@test.com");
        user.setPhone("4445556666");
        user.setHashedPassword("pass");
        User savedUser = userRepository.save(user);

        Account account = new Account();
        account.setUser(savedUser);
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepository.save(account);

        Wallet wallet = new Wallet();
        wallet.setAccount(savedAccount);
        wallet.setBalance(new BigDecimal("100.00"));
        Wallet savedWallet = walletRepository.save(wallet);

        // When/Then
        assertThrows(Exception.class, () -> {
            savedWallet.setBalance(new BigDecimal("-50.00"));
            walletRepository.save(savedWallet);
        });
    }
}