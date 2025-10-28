package com.brokerx.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestE2E {

    private static final String testEmail = "test-e2e-" + System.currentTimeMillis() + "@brokerx.com";
    private static String testAccountId;
    private static String testOtp;
    private static String jwtToken;
    private static String walletId;
    private static String orderId;

    @BeforeAll
    public static void setup() {
        
        String baseUrl = System.getProperty("api.base.url", "http://localhost:8080");
        RestAssured.baseURI = baseUrl;
        System.out.println("======================================================");
        System.out.println(" DÉMARRAGE DES TESTS E2E CONTRE L'API GATEWAY");
        System.out.println(" URL CIBLE : " + baseUrl);
        System.out.println("======================================================");
    }

    @Test
    @Order(1)
    @DisplayName("UC-01: Inscription - Créer un nouveau compte")
    public void uc01_shouldRegisterNewUser() {
        System.out.println("\n--- TEST 1: Inscription ---");
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", testEmail);
        registerRequest.put("phone", "5551234" + System.currentTimeMillis() % 10000);
        registerRequest.put("hashedPassword", "SecurePass123!");
        registerRequest.put("firstName", "John");
        registerRequest.put("lastName", "Doe");
        registerRequest.put("dateOfBirth", LocalDate.now().minusYears(25).toString());
        registerRequest.put("address", "123 Test Street, Montreal");

        Response response = given()
            .contentType(ContentType.JSON)
            .body(registerRequest)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("accountId", notNullValue())
            .body("otp", matchesPattern("\\d{6}"))
            .extract().response();
            
        testAccountId = response.path("accountId");
        testOtp = response.path("otp");
        
        System.out.println("✅ Inscription réussie. Account ID: " + testAccountId + ", OTP: " + testOtp);
    }

    @Test
    @Order(2)
    @DisplayName("UC-01: Vérification OTP - Activer le compte")
    public void uc01_shouldVerifyOtpAndActivateAccount() {
        System.out.println("\n--- TEST 2: Vérification OTP ---");
        Assumptions.assumeTrue(testAccountId != null && testOtp != null, "L'inscription doit avoir réussi");

        Map<String, String> otpRequest = new HashMap<>();
        otpRequest.put("accountId", testAccountId);
        otpRequest.put("otp", testOtp);

        given()
            .contentType(ContentType.JSON)
            .body(otpRequest)
        .when()
            .post("/auth/verify-otp")
        .then()
            .statusCode(200)
            .body(containsString("Compte activé avec succès"));
        
        System.out.println("✅ OTP vérifié - Compte activé.");
    }

    @Test
    @Order(3)
    @DisplayName("UC-02: Connexion - Authentification avec MFA")
    public void uc02_shouldLoginAndCompleteMfa() {
        System.out.println("\n--- TEST 3: Connexion (Login + MFA) ---");
        
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", testEmail);
        loginRequest.put("hashedPassword", "SecurePass123!");

        Response loginResponse = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("mfaRequired", equalTo(true))
            .body("tempToken", notNullValue())
            .extract().response();

        String tempToken = loginResponse.path("tempToken");
        String mfaMessage = loginResponse.path("message");
        String mfaOtp = mfaMessage.split("Code OTP: ")[1].trim();
        
        System.out.println("✅ Login initial réussi. MFA OTP: " + mfaOtp);

        Map<String, String> mfaRequest = new HashMap<>();
        mfaRequest.put("tempToken", tempToken);
        mfaRequest.put("otpCode", mfaOtp);

        jwtToken = given()
            .contentType(ContentType.JSON)
            .body(mfaRequest)
        .when()
            .post("/auth/verify-mfa")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("token", startsWith("eyJ"))
            .extract().path("token");
        
        System.out.println("✅ MFA vérifié - JWT obtenu.");
    }

    @Test
    @Order(4)
    @DisplayName("UC-03: Wallet - Récupérer et approvisionner")
    public void uc03_shouldGetWalletAndDepositFunds() {
        System.out.println("\n--- TEST 4: Opérations sur le Wallet ---");
        Assumptions.assumeTrue(jwtToken != null, "Le login MFA doit avoir réussi");

        Response walletResponse = given()
            .header("Authorization", "Bearer " + jwtToken)
        .when()
            .get("/wallets/my-wallet")
        .then()
            .statusCode(200)
            .body("balance", equalTo(0.0F)) // Les nombres JSON sont souvent des Floats/Doubles
            .extract().response();
        
        walletId = walletResponse.path("walletId");
        System.out.println("✅ Wallet récupéré. Wallet ID: " + walletId);

        // Étape 2: Déposer 5000 USD
        given()
            .header("Authorization", "Bearer " + jwtToken)
        .when()
            // L'endpoint attend des query parameters, pas un body
            .post("/wallets/deposit?walletId=" + walletId + "&amount=5000.00&paymentMethod=CARD")
        .then()
            .statusCode(200)
            .body("success", equalTo(true));
        
        System.out.println("✅ Dépôt de 5000.00$ réussi.");
    }

    @Test
    @Order(5)
    @DisplayName("UC-05: Ordre - Placer un ordre d'achat LIMIT")
    public void uc05_shouldPlaceLimitBuyOrder() {
        System.out.println("\n--- TEST 5: Placement d'un Ordre ---");
        Assumptions.assumeTrue(jwtToken != null, "Un JWT est requis");

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("clientOrderId", "E2E_TEST_" + System.currentTimeMillis());
        orderRequest.put("symbol", "AAPL");
        orderRequest.put("side", "BUY");
        orderRequest.put("orderType", "LIMIT");
        orderRequest.put("quantity", 10);
        orderRequest.put("price", new BigDecimal("150.00"));
        orderRequest.put("timeInForce", "DAY");

        orderId = given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body(orderRequest)
        .when()
            .post("/orders")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("status", equalTo("WORKING"))
            .extract().path("orderId");

        System.out.println("✅ Ordre placé avec succès. Order ID: " + orderId);
    }
    
    @Test
    @Order(6)
    @DisplayName("UC-06: Ordre - Annuler l'ordre placé")
    public void uc06_shouldCancelExistingOrder() {
        System.out.println("\n--- TEST 6: Annulation d'un Ordre ---");
        Assumptions.assumeTrue(jwtToken != null && orderId != null, "Un ordre doit avoir été placé");

        given()
            .header("Authorization", "Bearer " + jwtToken)
        .when()
            .delete("/orders/" + orderId)
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("action", equalTo("CANCELLED"));

        System.out.println("✅ Ordre " + orderId + " annulé avec succès.");
    }

    @Test
    @Order(7)
    @DisplayName("Conformité: Audit - Vérifier la présence des logs")
    public void testAuditLogs() {
        System.out.println("\n--- TEST 7: Vérification des Logs d'Audit ---");
        Assumptions.assumeTrue(jwtToken != null, "Un JWT est requis pour accéder à cet endpoint");

        given()
            .header("Authorization", "Bearer " + jwtToken)
        .when()
            .get("/audit/recent?limit=10")
        .then()
            .statusCode(200)
            .body("$", hasSize(greaterThan(5)));

        System.out.println("✅ Logs d'audit récupérés avec succès.");
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("\n======================================================");
        System.out.println(" FIN DES TESTS E2E - WORKFLOW COMPLET VALIDÉ");
        System.out.println("======================================================");
    }
}