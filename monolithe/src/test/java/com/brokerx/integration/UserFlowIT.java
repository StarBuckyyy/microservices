package com.brokerx.integration;

import com.brokerx.dto.auth.OtpVerifyRequest;
import com.brokerx.dto.auth.RegisterRequest;
import com.brokerx.dto.order.OrderRequest;
import com.brokerx.dto.order.OrderResponse;
import com.brokerx.entity.Account;
import com.brokerx.entity.Wallet;
import com.brokerx.repository.AccountRepository;
import com.brokerx.repository.OrderRepository;
import com.brokerx.repository.UserRepository;
import com.brokerx.repository.WalletRepository;
import com.brokerx.service.FundReservationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// Test E2E qui simule un parcours utilisateur complet :
//1. Inscription (UC-01)
//2. Vérification OTP (UC-01)
//3. Dépôt de fonds (UC-03)
//4. Placement d'un ordre (UC-05)

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) 
public class UserFlowIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private FundReservationService fundReservationService;

    private static String userEmail;
    private static String userPassword = "password123";
    private static UUID accountId;
    private static String otp;
    private static UUID walletId;

    @BeforeAll
    static void setup() {
        userEmail = "e2e-user-" + System.currentTimeMillis() + "@brokerx.com";
    }

    @Test
    @Order(1)
    void step1_registerUser_shouldSucceed() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail(userEmail);
        registerRequest.setHashedPassword(userPassword);
        registerRequest.setFirstName("E2E");
        registerRequest.setLastName("Test");
        registerRequest.setPhone("5551234567");
        registerRequest.setDateOfBirth("1990-01-01");
        registerRequest.setAddress("123 E2E St, Test City, TX");

        ResponseEntity<Map> response = testRestTemplate.postForEntity("/auth/register", registerRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        accountId = UUID.fromString((String) response.getBody().get("accountId"));
        otp = (String) response.getBody().get("otp");

        assertNotNull(accountId);
        assertNotNull(otp);

        assertTrue(userRepository.findByEmail(userEmail).isPresent(), "L'utilisateur devrait exister dans la BDD");
    }

    @Test
    @Order(2)
    void step2_verifyAccount_shouldActivateAccount() {
        assertNotNull(accountId, "accountId ne doit pas être null pour cette étape");
        assertNotNull(otp, "otp ne doit pas être null pour cette étape");

        OtpVerifyRequest verifyRequest = new OtpVerifyRequest();
        verifyRequest.setAccountId(accountId);
        verifyRequest.setOtp(otp);

        ResponseEntity<String> response = testRestTemplate.postForEntity("/auth/verify-otp", verifyRequest, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Compte activé avec succès"));

        Account account = accountRepository.findById(accountId).orElseThrow();
        assertEquals("ACTIVE", account.getStatus());
    }

    @Test
    @Order(3)
    void step3_depositFunds_shouldUpdateWalletBalance() {
        assertNotNull(accountId, "accountId ne doit pas être null pour cette étape");

        Wallet wallet = walletRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("Le wallet aurait dû être créé automatiquement"));
        walletId = wallet.getWalletId();

        URI depositUri = UriComponentsBuilder.fromPath("/wallets/deposit")
                .queryParam("walletId", walletId.toString())
                .queryParam("amount", 5000.00)
                .build().toUri();
        
        ResponseEntity<Map> response = testRestTemplate.postForEntity(depositUri, null, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));

        Wallet updatedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(0, new BigDecimal("5000.00").compareTo(updatedWallet.getBalance()), "Le solde du wallet doit être de 5000.00");
    }
    
    @Test
    @Order(4)
    void step4_placeBuyOrder_shouldSucceedAndReserveFunds() {
        assertNotNull(walletId, "walletId ne doit pas être null pour cette étape");

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setClientOrderId("E2E_BUY_ORDER_01");
        orderRequest.setSymbol("AAPL");
        orderRequest.setSide("BUY");
        orderRequest.setOrderType("LIMIT");
        orderRequest.setQuantity(10);
        orderRequest.setPrice(new BigDecimal("150.00")); 
        orderRequest.setTimeInForce("DAY");

        ResponseEntity<OrderResponse> response = testRestTemplate.postForEntity("/orders", orderRequest, OrderResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        OrderResponse orderResponse = response.getBody();
        assertNotNull(orderResponse);
        assertTrue(orderResponse.isSuccess());
        assertEquals("WORKING", orderResponse.getStatus());

        assertTrue(orderRepository.findById(orderResponse.getOrderId()).isPresent());
        
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(0, new BigDecimal("5000.00").compareTo(wallet.getBalance()));

        BigDecimal reservedAmount = fundReservationService.getTotalReserved(walletId);
        assertEquals(0, new BigDecimal("1500.00").compareTo(reservedAmount), "Les fonds réservés doivent être de 1500.00");

        BigDecimal availableBalance = fundReservationService.getAvailableBalance(wallet);
        assertEquals(0, new BigDecimal("3500.00").compareTo(availableBalance));
    }

} 