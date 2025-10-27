package com.brokerx.service;

import com.brokerx.dto.audit.CreateAuditLogRequest;
import com.brokerx.dto.order.OrderModificationResponse;
import com.brokerx.dto.order.OrderRequest;
import com.brokerx.dto.order.OrderResponse;
import com.brokerx.entity.Order;
import com.brokerx.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final OrderValidationService validationService;
    private final FundReservationService fundReservationService;
    private final AuditService auditService;
    private final RestTemplate restTemplate;
    
    private static final String ACCOUNT_SERVICE_URL = "http://account-service:8081";
    private static final String WALLET_SERVICE_URL = "http://wallet-service:8082";

    public OrderService(OrderRepository orderRepository, 
                       OrderValidationService validationService,
                       FundReservationService fundReservationService,
                       AuditService auditService,
                       RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.validationService = validationService;
        this.fundReservationService = fundReservationService;
        this.auditService = auditService;
        this.restTemplate = restTemplate;
    }
    
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("Placing order for user: {}", email);

        try {
            AccountWalletInfo info = getAccountWalletInfo(email);
            if (info == null) {
                return new OrderResponse("Failed to retrieve account information", false);
            }

            if (!"ACTIVE".equals(info.accountStatus)) {
                return new OrderResponse("Account is not active", false);
            }

            if (orderRepository.findByAccountIdAndClientOrderId(
                    info.accountId, request.getClientOrderId()).isPresent()) {
                return new OrderResponse("Order with this Client Order ID already exists", false);
            }

            var basicValidation = validationService.validateBasicFields(request);
            if (!basicValidation.isValid()) {
                return new OrderResponse(basicValidation.getMessage(), false);
            }

            BigDecimal reservationAmount = calculateReservationAmount(request);

            if ("BUY".equals(request.getSide())) {
                if (info.walletBalance.compareTo(reservationAmount) < 0) {
                    return new OrderResponse(
                        String.format("Insufficient funds. Required: %s, Available: %s", 
                                     reservationAmount, info.walletBalance), 
                        false
                    );
                }
            }

            Order order = createOrderFromRequest(request, info.accountId);
            Order savedOrder = orderRepository.save(order);

            if ("BUY".equals(savedOrder.getSide()) && 
                reservationAmount.compareTo(BigDecimal.ZERO) > 0) {
                
                boolean reserved = fundReservationService.reserveFunds(
                    savedOrder.getOrderId(), 
                    info.walletId, 
                    reservationAmount
                );
                
                if (!reserved) {
                    throw new RuntimeException("Failed to reserve funds");
                }
            }

            // ✅ AUDIT : Log création ordre
            auditService.logAction(
                "ORDER",
                savedOrder.getOrderId(),
                "CREATE",
                info.userId,
                "internal-order-service",
                Map.of(
                    "clientOrderId", savedOrder.getClientOrderId(),
                    "symbol", savedOrder.getSymbol(),
                    "side", savedOrder.getSide(),
                    "orderType", savedOrder.getOrderType(),
                    "quantity", savedOrder.getQuantity(),
                    "price", savedOrder.getPrice() != null ? savedOrder.getPrice().toString() : "MARKET",
                    "status", savedOrder.getStatus()
                )
            );

            logger.info("Order placed successfully: orderId={}, clientOrderId={}", 
                       savedOrder.getOrderId(), savedOrder.getClientOrderId());

            return new OrderResponse(savedOrder, "Order placed successfully");

        } catch (Exception e) {
            logger.error("Error placing order: {}", e.getMessage(), e);
            return new OrderResponse("Internal error while placing order: " + e.getMessage(), false);
        }
    }
    
    @Transactional
    public OrderModificationResponse cancelOrder(UUID orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return new OrderModificationResponse(false, "Order not found");
        }
        
        Order order = orderOpt.get();
        if (!Arrays.asList("NEW", "WORKING").contains(order.getStatus())) {
            return new OrderModificationResponse(
                false, 
                "Order cannot be cancelled in its current state: " + order.getStatus()
            );
        }

        String oldStatus = order.getStatus();
        UUID userId = null;

        // Libérer les fonds réservés
        if ("BUY".equals(order.getSide())) {
            try {
                String accountUrl = ACCOUNT_SERVICE_URL + "/accounts/" + order.getAccountId();
                Map<String, Object> accountResponse = restTemplate.getForObject(accountUrl, Map.class);
                if (accountResponse != null) {
                    Object userIdObj = accountResponse.get("userId");
                    if (userIdObj != null) {
                        userId = UUID.fromString(userIdObj.toString());
                    }
                    
                    String walletUrl = WALLET_SERVICE_URL + "/wallets/account/" + order.getAccountId();
                    Map<String, Object> walletResponse = restTemplate.getForObject(walletUrl, Map.class);
                    if (walletResponse != null) {
                        Object walletIdObj = walletResponse.get("walletId");
                        if (walletIdObj != null) {
                            UUID walletId = UUID.fromString(walletIdObj.toString());
                            fundReservationService.releaseFunds(order.getOrderId(), walletId);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error releasing funds during cancellation: {}", e.getMessage());
            }
        }
        
        order.setStatus("CANCELLED");
        Order cancelledOrder = orderRepository.save(order);
        
        // ✅ AUDIT : Log annulation ordre
        auditService.logAction(
            "ORDER",
            cancelledOrder.getOrderId(),
            "DELETE",
            userId,
            "internal-order-service",
            Map.of(
                "clientOrderId", cancelledOrder.getClientOrderId(),
                "oldStatus", oldStatus,
                "newStatus", "CANCELLED"
            )
        );
        
        return new OrderModificationResponse(
            true, 
            "Order cancelled", 
            new OrderResponse(cancelledOrder, null), 
            "CANCELLED"
        );
    }

    @Transactional
    public OrderModificationResponse modifyOrder(UUID orderId, Integer newQuantity, BigDecimal newPrice) {
        logger.info("Modifying order: orderId={}, newQuantity={}, newPrice={}", orderId, newQuantity, newPrice);
        
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return new OrderModificationResponse(false, "Order not found");
        }
        
        Order order = orderOpt.get();
        
        if (!Arrays.asList("NEW", "WORKING").contains(order.getStatus())) {
            return new OrderModificationResponse(
                false, 
                "Order cannot be modified in its current state: " + order.getStatus()
            );
        }
        
        if (order.getFilledQuantity() > 0) {
            return new OrderModificationResponse(
                false, 
                "Cannot modify a partially filled order. Filled: " + order.getFilledQuantity()
            );
        }
        
        if ("MARKET".equals(order.getOrderType())) {
            return new OrderModificationResponse(
                false, 
                "Cannot modify MARKET orders. Please cancel and create a new order."
            );
        }
        
        if (newQuantity != null && newQuantity <= 0) {
            return new OrderModificationResponse(false, "New quantity must be positive");
        }
        
        if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new OrderModificationResponse(false, "New price must be positive");
        }
        
        Integer oldQuantity = order.getQuantity();
        BigDecimal oldPrice = order.getPrice();
        UUID userId = null;

        if ("BUY".equals(order.getSide())) {
            try {
                String accountUrl = ACCOUNT_SERVICE_URL + "/accounts/" + order.getAccountId();
                Map<String, Object> accountResponse = restTemplate.getForObject(accountUrl, Map.class);
                if (accountResponse == null) {
                    return new OrderModificationResponse(false, "Account not found");
                }
                
                // ✅ Extraction sécurisée du userId
                Object userIdObj = accountResponse.get("userId");
                if (userIdObj != null) {
                    userId = UUID.fromString(userIdObj.toString());
                }
                
                String walletUrl = WALLET_SERVICE_URL + "/wallets/account/" + order.getAccountId();
                Map<String, Object> walletResponse = restTemplate.getForObject(walletUrl, Map.class);
                if (walletResponse == null) {
                    return new OrderModificationResponse(false, "Wallet not found");
                }
                
                Object walletIdObj = walletResponse.get("walletId");
                if (walletIdObj == null) {
                    return new OrderModificationResponse(false, "Invalid wallet response");
                }
                UUID walletId = UUID.fromString(walletIdObj.toString());
                
                Object balanceObj = walletResponse.get("balance");
                BigDecimal walletBalance = balanceObj instanceof Number 
                    ? BigDecimal.valueOf(((Number) balanceObj).doubleValue())
                    : new BigDecimal(balanceObj.toString());
                
                BigDecimal oldReservation = fundReservationService.getReservedAmount(order.getOrderId());
                
                Integer finalQuantity = newQuantity != null ? newQuantity : order.getQuantity();
                BigDecimal finalPrice = newPrice != null ? newPrice : order.getPrice();
                BigDecimal newReservation = new BigDecimal(finalQuantity).multiply(finalPrice);
                
                BigDecimal reservationDelta = newReservation.subtract(oldReservation);
                
                logger.info("Reservation calculation: old={}, new={}, delta={}, walletBalance={}", 
                           oldReservation, newReservation, reservationDelta, walletBalance);
                
                if (reservationDelta.compareTo(BigDecimal.ZERO) > 0) {
                    if (walletBalance.compareTo(reservationDelta) < 0) {
                        return new OrderModificationResponse(
                            false, 
                            String.format("Insufficient funds for modification. Additional required: %s, Available: %s", 
                                         reservationDelta, walletBalance)
                        );
                    }
                }
                
                fundReservationService.releaseFunds(order.getOrderId(), walletId);
                
                if (newQuantity != null) {
                    order.setQuantity(newQuantity);
                }
                
                if (newPrice != null) {
                    order.setPrice(newPrice);
                }
                
                Order modifiedOrder = orderRepository.save(order);
                
                boolean reserved = fundReservationService.reserveFunds(
                    modifiedOrder.getOrderId(), 
                    walletId, 
                    newReservation
                );
                
                if (!reserved) {
                    logger.error("Failed to reserve new funds, rolling back");
                    fundReservationService.reserveFunds(order.getOrderId(), walletId, oldReservation);
                    return new OrderModificationResponse(false, "Failed to reserve funds for modification");
                }
                
                // ✅ AUDIT : Log modification ordre
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("clientOrderId", modifiedOrder.getClientOrderId());
                if (newQuantity != null) {
                    auditDetails.put("oldQuantity", oldQuantity);
                    auditDetails.put("newQuantity", newQuantity);
                }
                if (newPrice != null) {
                    auditDetails.put("oldPrice", oldPrice.toString());
                    auditDetails.put("newPrice", newPrice.toString());
                }
                
                auditService.logAction(
                    "ORDER",
                    modifiedOrder.getOrderId(),
                    "UPDATE",
                    userId,
                    "internal-order-service",
                    auditDetails
                );
                
                logger.info("Order modified successfully: orderId={}, newQuantity={}, newPrice={}", 
                           orderId, finalQuantity, finalPrice);
                
                return new OrderModificationResponse(
                    true, 
                    String.format("Order modified successfully. New quantity: %d, New price: %s", 
                                 finalQuantity, finalPrice),
                    new OrderResponse(modifiedOrder, null), 
                    "MODIFIED"
                );
                
            } catch (Exception e) {
                logger.error("Error modifying order: {}", e.getMessage(), e);
                return new OrderModificationResponse(
                    false, 
                    "Error during modification: " + e.getMessage()
                );
            }
            
        } else {
            // SELL orders : pas de réservation de fonds
            if (newQuantity != null) {
                order.setQuantity(newQuantity);
            }
            
            if (newPrice != null) {
                order.setPrice(newPrice);
            }
            
            Order modifiedOrder = orderRepository.save(order);
            
            // Récupérer userId pour l'audit
            try {
                String accountUrl = ACCOUNT_SERVICE_URL + "/accounts/" + order.getAccountId();
                Map<String, Object> accountResponse = restTemplate.getForObject(accountUrl, Map.class);
                if (accountResponse != null) {
                    Object userIdObj = accountResponse.get("userId");
                    if (userIdObj != null) {
                        userId = UUID.fromString(userIdObj.toString());
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not fetch userId for audit: {}", e.getMessage());
            }
            
            // ✅ AUDIT : Log modification ordre SELL
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("clientOrderId", modifiedOrder.getClientOrderId());
            if (newQuantity != null) {
                auditDetails.put("oldQuantity", oldQuantity);
                auditDetails.put("newQuantity", newQuantity);
            }
            if (newPrice != null) {
                auditDetails.put("oldPrice", oldPrice.toString());
                auditDetails.put("newPrice", newPrice.toString());
            }
            
            auditService.logAction(
                "ORDER",
                modifiedOrder.getOrderId(),
                "MODIFY",
                userId,
                "internal-order-service",
                auditDetails
            );
            
            logger.info("SELL order modified successfully: orderId={}", orderId);
            
            return new OrderModificationResponse(
                true, 
                "Order modified successfully",
                new OrderResponse(modifiedOrder, null), 
                "MODIFIED"
            );
        }
    }

    // Méthodes auxiliaires (inchangées)
    private AccountWalletInfo getAccountWalletInfo(String email) {
        try {
            logger.info("Fetching account/wallet info for email: {}", email);
            
            String userUrl = ACCOUNT_SERVICE_URL + "/users/email/" + email;
            ResponseEntity<Map> userResponseEntity = restTemplate.getForEntity(userUrl, Map.class);
            Map<String, Object> userResponse = userResponseEntity.getBody();
            
            if (userResponse == null) {
                logger.error("User not found for email: {}", email);
                return null;
            }
            
            Object userIdObj = userResponse.get("userId");
            if (userIdObj == null) {
                logger.error("userId is null in user response");
                return null;
            }
            UUID userId = UUID.fromString(userIdObj.toString());
            logger.info("Found userId: {}", userId);

            String accountUrl = ACCOUNT_SERVICE_URL + "/accounts/user/" + userId;
            ResponseEntity<Map> accountResponseEntity = restTemplate.getForEntity(accountUrl, Map.class);
            Map<String, Object> accountResponse = accountResponseEntity.getBody();
            
            if (accountResponse == null) {
                logger.error("Account not found for userId: {}", userId);
                return null;
            }
            
            Object accountIdObj = accountResponse.get("accountId");
            if (accountIdObj == null) {
                logger.error("accountId is null in account response");
                return null;
            }
            UUID accountId = UUID.fromString(accountIdObj.toString());
            String accountStatus = (String) accountResponse.get("status");
            logger.info("Found accountId: {}, status: {}", accountId, accountStatus);

            String walletUrl = WALLET_SERVICE_URL + "/wallets/account/" + accountId;
            ResponseEntity<Map> walletResponseEntity = restTemplate.getForEntity(walletUrl, Map.class);
            Map<String, Object> walletResponse = walletResponseEntity.getBody();
            
            if (walletResponse == null) {
                logger.error("Wallet not found for accountId: {}", accountId);
                return null;
            }
            
            Object walletIdObj = walletResponse.get("walletId");
            if (walletIdObj == null) {
                logger.error("walletId is null in wallet response");
                return null;
            }
            UUID walletId = UUID.fromString(walletIdObj.toString());
            
            Object balanceObj = walletResponse.get("balance");
            BigDecimal balance = balanceObj instanceof Number 
                ? BigDecimal.valueOf(((Number) balanceObj).doubleValue())
                : new BigDecimal(balanceObj.toString());
            
            logger.info("Found walletId: {}, balance: {}", walletId, balance);

            return new AccountWalletInfo(userId, accountId, accountStatus, walletId, balance);
            
        } catch (Exception e) {
            logger.error("Failed to fetch account/wallet info for email {}: {}", email, e.getMessage(), e);
            return null;
        }
    }

    private BigDecimal calculateReservationAmount(OrderRequest request) {
        if (!"BUY".equals(request.getSide())) {
            return BigDecimal.ZERO;
        }
        
        if ("MARKET".equals(request.getOrderType())) {
            return new BigDecimal(request.getQuantity()).multiply(new BigDecimal("100.00"));
        } else if ("LIMIT".equals(request.getOrderType())) {
            return new BigDecimal(request.getQuantity()).multiply(request.getPrice());
        }
        
        return BigDecimal.ZERO;
    }

    private Order createOrderFromRequest(OrderRequest request, UUID accountId) {
        Order order = new Order();
        order.setAccountId(accountId);
        order.setClientOrderId(request.getClientOrderId());
        order.setSymbol(request.getSymbol().toUpperCase());
        order.setSide(request.getSide());
        order.setOrderType(request.getOrderType());
        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setTimeInForce(request.getTimeInForce());
        order.setStatus("WORKING");
        order.setFilledQuantity(0);
        return order;
    }

    public List<OrderResponse> getOrdersForUser(String email) {
        try {
            AccountWalletInfo info = getAccountWalletInfo(email);
            if (info == null) {
                return Collections.emptyList();
            }

            return orderRepository.findByAccountId(info.accountId)
                    .stream()
                    .map(order -> new OrderResponse(order, "Order found"))
                    .toList();
        } catch (Exception e) {
            logger.error("Error fetching orders for user: {}", email, e);
            return Collections.emptyList();
        }
    }

    public OrderResponse getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(order -> new OrderResponse(order, "Order found"))
                .orElse(new OrderResponse("Order not found", false));
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(order -> new OrderResponse(order, "Order found"))
                .toList();
    }

    private record AccountWalletInfo(
        UUID userId, 
        UUID accountId, 
        String accountStatus, 
        UUID walletId, 
        BigDecimal walletBalance
    ) {}
}