package com.brokerx.service;

import com.brokerx.dto.order.OrderRequest;
import com.brokerx.dto.order.OrderResponse;
import com.brokerx.dto.order.OrderModificationResponse;
import com.brokerx.entity.Account;
import com.brokerx.entity.Order;
import com.brokerx.entity.Wallet;
import com.brokerx.entity.User;
import com.brokerx.repository.OrderRepository;
import com.brokerx.service.OrderValidationService.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final AccountService accountService;
    private final WalletService walletService;
    private final OrderValidationService validationService;
    private final FundReservationService fundReservationService;
    private final UserService userService;
    private final AuditService auditService;

    public OrderService(OrderRepository orderRepository, AccountService accountService, 
                       WalletService walletService, OrderValidationService validationService,
                       FundReservationService fundReservationService, UserService userService,
                       AuditService auditService) {
        this.orderRepository = orderRepository;
        this.accountService = accountService;
        this.walletService = walletService;
        this.validationService = validationService;
        this.fundReservationService = fundReservationService;
        this.userService = userService;
        this.auditService = auditService;
    }

    public List<OrderResponse> getOrdersForUser(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return Collections.emptyList();
        }

        Account account = accountService.getAccountByUserId(user.getUserId());
        if (account == null) {
            return Collections.emptyList();
        }

        return orderRepository.findByAccount_AccountId(account.getAccountId())
                .stream()
                .map(order -> new OrderResponse(order, "Order retrieved successfully"))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        // ✅ FIX : Récupérer le compte de l'utilisateur CONNECTÉ
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("🔍 Placing order for authenticated user: {}", email);
        
        User user = userService.findByEmail(email);
        if (user == null) {
            logger.error("❌ User not found for email: {}", email);
            return new OrderResponse("User not found", false);
        }
        
        Account account = accountService.getAccountByUserId(user.getUserId());
        if (account == null) {
            logger.error("❌ Account not found for user: {}", user.getUserId());
            return new OrderResponse("Account not found", false);
        }
        
        if (!"ACTIVE".equals(account.getStatus())) {
            logger.error("❌ Account not active for user: {}, status: {}", user.getUserId(), account.getStatus());
            return new OrderResponse("Account is not active", false);
        }
        
        logger.info("✅ Order will be placed for: userId={}, accountId={}, email={}", 
                   user.getUserId(), account.getAccountId(), email);

        try {
            Optional<Order> existingOrder = orderRepository.findByAccount_AccountIdAndClientOrderId(
                account.getAccountId(), request.getClientOrderId());
            
            if (existingOrder.isPresent()) {
                logger.warn("Duplicate order detected: accountId={}, clientOrderId={}", 
                           account.getAccountId(), request.getClientOrderId());
                return new OrderResponse(existingOrder.get(), "Order already exists (idempotent)");
            }

            Wallet wallet = walletService.getWalletByAccountId(account.getAccountId());
            if (wallet == null) {
                return new OrderResponse("Wallet not found for account", false);
            }

            Order tempOrder = createOrderFromRequest(request, account);
            BigDecimal reservationAmount = fundReservationService.calculateReservationAmount(tempOrder);
            
            ValidationResult validation = validationService.validateOrder(request, account, wallet, fundReservationService);
            if (!validation.isValid()) {
                logger.warn("Order validation failed: clientOrderId={}, reason={}", 
                           request.getClientOrderId(), validation.getMessage());
                return new OrderResponse("Order rejected: " + validation.getMessage(), false);
            }

            if ("BUY".equals(request.getSide()) && reservationAmount.compareTo(BigDecimal.ZERO) > 0) {
                if (!fundReservationService.hasSufficientFunds(wallet, reservationAmount)) {
                    return new OrderResponse("Insufficient available funds after considering existing orders", false);
                }
            }

            Order savedOrder = orderRepository.save(tempOrder);

            if ("BUY".equals(request.getSide()) && reservationAmount.compareTo(BigDecimal.ZERO) > 0) {
                boolean reserved = fundReservationService.reserveFunds(
                    savedOrder.getOrderId(), wallet.getWalletId(), reservationAmount);
                
                if (!reserved) {
                    orderRepository.delete(savedOrder);
                    return new OrderResponse("Failed to reserve funds", false);
                }
                
                logger.info("Funds reserved: amount={}, orderId={}, availableAfter={}", 
                           reservationAmount, savedOrder.getOrderId(),
                           fundReservationService.getAvailableBalance(wallet));
            }

            // AUDIT : Log création ordre
            auditService.logOrderCreation(
                savedOrder.getOrderId(),
                user.getUserId(),
                request.getSymbol(),
                request.getSide(),
                request.getQuantity(),
                "127.0.0.1"
            );

            logger.info("✅ Order placed successfully: orderId={}, clientOrderId={}, status={}, reserved={}, userId={}, accountId={}", 
                       savedOrder.getOrderId(), savedOrder.getClientOrderId(), 
                       savedOrder.getStatus(), reservationAmount, user.getUserId(), account.getAccountId());

            return new OrderResponse(savedOrder, "Order placed successfully");

        } catch (Exception e) {
            logger.error("Error placing order: clientOrderId={}", request.getClientOrderId(), e);
            return new OrderResponse("Internal error while placing order: " + e.getMessage(), false);
        }
    }

    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(order -> new OrderResponse(order, "Order retrieved successfully"))
                .collect(Collectors.toList());
    }

    
    public OrderResponse getOrderById(UUID orderId) {
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isPresent()) {
            return new OrderResponse(order.get(), "Order found");
        } else {
            return new OrderResponse("Order not found", false);
        }
    }

    @Transactional
    public OrderModificationResponse cancelOrder(UUID orderId) {
        logger.info("Attempting to cancel order: orderId={}", orderId);
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                return new OrderModificationResponse(false, "Order not found");
            }
            
            Order order = orderOpt.get();
            
            if ("FILLED".equals(order.getStatus())) {
                return new OrderModificationResponse(false, "Cannot cancel a fully filled order");
            }
            
            if ("CANCELLED".equals(order.getStatus())) {
                return new OrderModificationResponse(false, "Order is already cancelled");
            }
            
            if ("BUY".equals(order.getSide())) {
                Wallet wallet = walletService.getWalletByAccountId(order.getAccount().getAccountId());
                if (wallet != null) {
                    fundReservationService.releaseFunds(order.getOrderId(), wallet.getWalletId());
                    logger.info("Funds released for cancelled order: orderId={}, walletId={}", 
                            orderId, wallet.getWalletId());
                }
            }
            
            order.setStatus("CANCELLED");
            Order cancelledOrder = orderRepository.save(order);
            
            // AUDIT : Log annulation ordre
            auditService.logOrderCancellation(
                orderId,
                order.getAccount().getUser().getUserId(),
                "127.0.0.1"
            );
            
            logger.info("Order cancelled successfully: orderId={}", orderId);
            
            OrderResponse orderResponse = new OrderResponse(cancelledOrder, "Order cancelled successfully");
            return new OrderModificationResponse(true, "Order cancelled successfully", orderResponse, "CANCELLED");
            
        } catch (Exception e) {
            logger.error("Error cancelling order: orderId={}", orderId, e);
            return new OrderModificationResponse(false, "Error cancelling order: " + e.getMessage());
        }
    }

    @Transactional
    public OrderModificationResponse modifyOrder(UUID orderId, Integer newQuantity, BigDecimal newPrice) {
        logger.info("Attempting to modify order: orderId={}, newQuantity={}, newPrice={}", 
                orderId, newQuantity, newPrice);
        
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                return new OrderModificationResponse(false, "Order not found");
            }
            
            Order order = orderOpt.get();
            
            if ("FILLED".equals(order.getStatus())) {
                return new OrderModificationResponse(false, "Cannot modify a fully filled order");
            }
            
            if ("CANCELLED".equals(order.getStatus())) {
                return new OrderModificationResponse(false, "Cannot modify a cancelled order");
            }

            // AUDIT : Sauvegarder l'ancien état
            Map<String, Object> oldValues = new HashMap<>();
            oldValues.put("quantity", order.getQuantity());
            oldValues.put("price", order.getPrice());
            oldValues.put("status", order.getStatus());

            Integer currentFilledQuantity = order.getFilledQuantity();
            
            if (newQuantity != null && newQuantity < currentFilledQuantity) {
                return new OrderModificationResponse(false, 
                    "New quantity cannot be less than already filled quantity (" + currentFilledQuantity + ")");
            }
            
            BigDecimal oldReservation = fundReservationService.getReservedAmount(orderId);
            BigDecimal newReservation = BigDecimal.ZERO;
            
            if ("BUY".equals(order.getSide())) {
                Wallet wallet = walletService.getWalletByAccountId(order.getAccount().getAccountId());
                if (wallet == null) {
                    return new OrderModificationResponse(false, "Wallet not found");
                }
                
                Integer finalQuantity = newQuantity != null ? newQuantity : order.getQuantity();
                BigDecimal finalPrice = newPrice != null ? newPrice : order.getPrice();
                
                if ("LIMIT".equals(order.getOrderType())) {
                    newReservation = new BigDecimal(finalQuantity).multiply(finalPrice);
                } else {
                    newReservation = new BigDecimal(finalQuantity).multiply(new BigDecimal("100.00"));
                }
                
                fundReservationService.releaseFunds(orderId, wallet.getWalletId());
                
                if (!fundReservationService.hasSufficientFunds(wallet, newReservation)) {
                    fundReservationService.reserveFunds(orderId, wallet.getWalletId(), oldReservation);
                    return new OrderModificationResponse(false, "Insufficient funds for modification");
                }
                
                boolean reserved = fundReservationService.reserveFunds(orderId, wallet.getWalletId(), newReservation);
                if (!reserved) {
                    fundReservationService.reserveFunds(orderId, wallet.getWalletId(), oldReservation);
                    return new OrderModificationResponse(false, "Failed to reserve funds for modification");
                }
                
                logger.info("Funds adjusted for modified order: orderId={}, oldReservation={}, newReservation={}", 
                        orderId, oldReservation, newReservation);
            }
            
            if (newQuantity != null) {
                order.setQuantity(newQuantity);
                order.setFilledQuantity(currentFilledQuantity);
            }
            
            if (newPrice != null && "LIMIT".equals(order.getOrderType())) {
                order.setPrice(newPrice);
            }
            
            order.setStatus("WORKING");
            Order modifiedOrder = orderRepository.save(order);

            // AUDIT : Log modification ordre
            Map<String, Object> newValues = new HashMap<>();
            newValues.put("quantity", modifiedOrder.getQuantity());
            newValues.put("price", modifiedOrder.getPrice());
            newValues.put("status", modifiedOrder.getStatus());
            
            auditService.logOrderModification(
                orderId,
                order.getAccount().getUser().getUserId(),
                "127.0.0.1",
                oldValues,
                newValues
            );
            
            logger.info("Order modified successfully: orderId={}", orderId);
            
            OrderResponse orderResponse = new OrderResponse(modifiedOrder, "Order modified successfully");
            return new OrderModificationResponse(true, "Order modified successfully", orderResponse, "MODIFIED");
            
        } catch (Exception e) {
            logger.error("Error modifying order: orderId={}", orderId, e);
            return new OrderModificationResponse(false, "Error modifying order: " + e.getMessage());
        }
    }

    private Order createOrderFromRequest(OrderRequest request, Account account) {
        Order order = new Order();
        order.setAccount(account);
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
}