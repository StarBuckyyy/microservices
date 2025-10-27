package com.brokerx.controller;

import com.brokerx.config.JwtConfig;
import com.brokerx.dto.auth.*;
import com.brokerx.entity.User;
import com.brokerx.entity.Account;
import com.brokerx.service.UserService;
import com.brokerx.service.AccountService;
import com.brokerx.service.OtpService;
import com.brokerx.service.AuditService; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AccountService accountService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final AuditService auditService; 
    
    private final Map<String, MfaSession> mfaSessions = new ConcurrentHashMap<>();

    public AuthController(UserService userService, AccountService accountService, 
                         OtpService otpService, PasswordEncoder passwordEncoder, 
                         JwtConfig jwtConfig, AuditService auditService) { 
        this.userService = userService;
        this.accountService = accountService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.auditService = auditService; 
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) { 
        
        logger.info("Registration attempt for email: {}", request.getEmail());
        
        try {
            if (userService.findByEmail(request.getEmail()) != null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email déjà utilisé"));
            }

            User user = new User();
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setHashedPassword(passwordEncoder.encode(request.getHashedPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            
            if (request.getDateOfBirth() != null && !request.getDateOfBirth().isEmpty()) {
                user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
            }
            
            user.setAddress(request.getAddress());
            User savedUser = userService.createUser(user);

            auditService.logAction(
                "USER", 
                savedUser.getUserId(), 
                "CREATE", 
                null,
                getClientIp(httpRequest),
                Map.of(
                    "email", savedUser.getEmail(),
                    "firstName", savedUser.getFirstName(),
                    "lastName", savedUser.getLastName()
                )
            );

            Account account = new Account();
            account.setUser(savedUser);
            account.setStatus("PENDING");
            Account savedAccount = accountService.saveAccount(account);
            
            auditService.logAction(
                "ACCOUNT", 
                savedAccount.getAccountId(), 
                "CREATE", 
                savedUser.getUserId(),
                getClientIp(httpRequest),
                Map.of("status", "PENDING")
            );
            
            String otp = otpService.generateOtp(savedAccount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accountId", savedAccount.getAccountId());
            response.put("message", "Inscription réussie ! Un code OTP a été généré.");
            response.put("otp", otp);

            logger.info("User registered successfully: userId={}, accountId={}", 
                       savedUser.getUserId(), savedAccount.getAccountId());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Registration error for email: {}", request.getEmail(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de l'inscription: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestBody OtpVerifyRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("OTP verification attempt for accountId: {}", request.getAccountId());
        
        try {
            Account account = accountService.getAccountById(request.getAccountId());
            if (account == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (!"PENDING".equals(account.getStatus())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Compte déjà actif"));
            }

            boolean valid = otpService.verifyOtp(account, request.getOtp());
            if (valid) {
                auditService.logAction(
                    "ACCOUNT", 
                    account.getAccountId(), 
                    "UPDATE", 
                    account.getUser().getUserId(),
                    getClientIp(httpRequest),
                    Map.of("status", "ACTIVE", "verificationMethod", "OTP")
                );
                
                logger.info("OTP verified successfully for accountId: {}", request.getAccountId());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Compte activé avec succès !"
                ));
            }
            
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Code OTP invalide ou expiré"));
                
        } catch (Exception e) {
            logger.error("OTP verification error for accountId: {}", request.getAccountId(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false, "message", "Erreur lors de la vérification: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("Login attempt for email: {}", request.getEmail());
        
        try {
            User user = userService.findByEmail(request.getEmail());
            if (user == null) {
                logger.warn("Login failed - user not found: {}", request.getEmail());
                return ResponseEntity.status(404)
                    .body(new LoginResponse(false, "Utilisateur non trouvé"));
            }

            Account account = accountService.getAccountByUserId(user.getUserId());
            if (account == null) {
                logger.warn("Login failed - no account for user: {}", request.getEmail());
                return ResponseEntity.status(404)
                    .body(new LoginResponse(false, "Compte non trouvé"));
            }

            if ("PENDING".equals(account.getStatus())) {
                logger.warn("Login failed - account pending: {}", request.getEmail());
                return ResponseEntity.status(403)
                    .body(new LoginResponse(false, "Compte en attente de vérification OTP"));
            }

            if (!passwordEncoder.matches(request.getHashedPassword(), user.getHashedPassword())) {
                logger.warn("Login failed - invalid password for: {}", request.getEmail());
                
                auditService.logAction(
                    "USER", 
                    user.getUserId(), 
                    "LOGIN_FAILED", 
                    user.getUserId(),
                    getClientIp(httpRequest),
                    Map.of("reason", "Invalid password")
                );
                
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Mot de passe incorrect"));
            }

            auditService.logLogin(
                user.getUserId(), 
                getClientIp(httpRequest), 
                httpRequest.getHeader("User-Agent")
            );

            String otp = otpService.generateOtp(account);
            String tempToken = java.util.UUID.randomUUID().toString();
            
            mfaSessions.put(tempToken, new MfaSession(user.getUserId(), user.getEmail(), otp));
            
            LoginResponse response = new LoginResponse(true, "MFA requis");
            response.setMfaRequired(true);
            response.setTempToken(tempToken);
            response.setMessage("MFA requis. Code OTP: " + otp);
            
            logger.info("MFA initiated for user: {}, tempToken: {}, otp: {}", 
                       request.getEmail(), tempToken, otp);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Login error for email: {}", request.getEmail(), e);
            return ResponseEntity.internalServerError()
                .body(new LoginResponse(false, "Erreur lors de la connexion: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<LoginResponse> verifyMfa(
            @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest) {
        
        logger.info("MFA verification attempt with tempToken: {}", request.getTempToken());
        
        try {
            MfaSession session = mfaSessions.get(request.getTempToken());
            if (session == null) {
                logger.warn("MFA failed - invalid temp token: {}", request.getTempToken());
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Session MFA invalide ou expirée"));
            }

            User user = userService.getUser(session.getUserId());
            if (user == null) {
                return ResponseEntity.status(404)
                    .body(new LoginResponse(false, "Utilisateur non trouvé"));
            }

            Account account = accountService.getAccountByUserId(user.getUserId());
            
            boolean valid = otpService.verifyOtp(account, request.getOtpCode());
            if (!valid) {
                logger.warn("MFA failed - invalid OTP for user: {}", session.getEmail());
                
                auditService.logMfaAttempt(
                    user.getUserId(), 
                    getClientIp(httpRequest), 
                    false
                );
                
                return ResponseEntity.status(401)
                    .body(new LoginResponse(false, "Code OTP invalide"));
            }

            mfaSessions.remove(request.getTempToken());

            auditService.logMfaAttempt(
                user.getUserId(), 
                getClientIp(httpRequest), 
                true
            );

            String token = jwtConfig.generateToken(user.getEmail(), user.getUserId(), "CLIENT");
            
            logger.info("MFA successful for user: {}", user.getEmail());
            
            return ResponseEntity.ok(new LoginResponse(
                true, 
                "Connexion réussie",
                token,
                user.getEmail(),
                user.getUserId().toString()
            ));
            
        } catch (Exception e) {
            logger.error("MFA verification error", e);
            return ResponseEntity.internalServerError()
                .body(new LoginResponse(false, "Erreur lors de la vérification MFA: " + e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private static class MfaSession {
        private final java.util.UUID userId;
        private final String email;
        private final String otp;
        private final long createdAt;

        public MfaSession(java.util.UUID userId, String email, String otp) {
            this.userId = userId;
            this.email = email;
            this.otp = otp;
            this.createdAt = System.currentTimeMillis();
        }

        public java.util.UUID getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getOtp() { return otp; }
        public long getCreatedAt() { return createdAt; }
    }
}