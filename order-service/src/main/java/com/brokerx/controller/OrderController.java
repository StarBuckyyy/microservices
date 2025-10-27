package com.brokerx.controller;

import com.brokerx.dto.order.OrderRequest;
import com.brokerx.dto.order.OrderResponse;
import com.brokerx.service.FundReservationService;
import com.brokerx.service.OrderService;
import com.brokerx.dto.order.CancelOrderRequest;
import com.brokerx.dto.order.ModifyOrderRequest;
import com.brokerx.dto.order.OrderModificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    private final FundReservationService fundReservationService;

    public OrderController(OrderService orderService, FundReservationService fundReservationService) {
        this.orderService = orderService;
        this.fundReservationService = fundReservationService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        logger.info("Received order placement request: clientOrderId={}, symbol={}, side={}", 
                   request.getClientOrderId(), request.getSymbol(), request.getSide());

        if (request.getClientOrderId() == null || request.getClientOrderId().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new OrderResponse("Client Order ID is required", false));
        }

        OrderResponse response = orderService.placeOrder(request);
        
        if (response.isSuccess()) {
            logger.info("Order placed successfully: orderId={}, clientOrderId={}", 
                       response.getOrderId(), response.getClientOrderId());
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Order placement failed: clientOrderId={}, reason={}", 
                       request.getClientOrderId(), response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(Authentication authentication) {
         if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        List<OrderResponse> orders = orderService.getOrdersForUser(authentication.getName());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/reservation-stats")
    public ResponseEntity<Map<String, Object>> getReservationStats() {
        Map<String, Object> stats = fundReservationService.getReservationStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugInfo() {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            debug.put("reservationStats", fundReservationService.getReservationStats());
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            logger.error("Error in debug endpoint", e);
        }
        
        return ResponseEntity.ok(debug);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderModificationResponse> cancelOrder(@PathVariable UUID orderId) {
        logger.info("Cancel order request received: orderId={}", orderId);
        
        OrderModificationResponse response = orderService.cancelOrder(orderId);
        
        if (response.isSuccess()) {
            logger.info("Order cancelled successfully: orderId={}", orderId);
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Order cancellation failed: orderId={}, reason={}", orderId, response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<OrderModificationResponse> cancelOrderWithBody(@RequestBody CancelOrderRequest request) {
        logger.info("Cancel order request received: orderId={}, clientOrderId={}", 
                request.getOrderId(), request.getClientOrderId());
        
        UUID orderId = request.getOrderId();
        if (orderId == null) {
            return ResponseEntity.badRequest()
                .body(new OrderModificationResponse(false, "Order ID is required"));
        }
        
        OrderModificationResponse response = orderService.cancelOrder(orderId);
        
        if (response.isSuccess()) {
            logger.info("Order cancelled successfully: orderId={}", orderId);
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Order cancellation failed: orderId={}, reason={}", orderId, response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderModificationResponse> modifyOrder(
            @PathVariable UUID orderId,
            @RequestBody ModifyOrderRequest request) {
        
        logger.info("Modify order request received: orderId={}, newQuantity={}, newPrice={}", 
                orderId, request.getNewQuantity(), request.getNewPrice());
        
        if (request.getNewQuantity() == null && request.getNewPrice() == null) {
            return ResponseEntity.badRequest()
                .body(new OrderModificationResponse(false, "At least one field (quantity or price) must be provided"));
        }
        
        OrderModificationResponse response = orderService.modifyOrder(
            orderId, 
            request.getNewQuantity(), 
            request.getNewPrice()
        );
        
        if (response.isSuccess()) {
            logger.info("Order modified successfully: orderId={}", orderId);
            return ResponseEntity.ok(response);
        } else {
            logger.warn("Order modification failed: orderId={}, reason={}", orderId, response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderModificationResponse> updateOrder(
            @PathVariable UUID orderId,
            @RequestBody ModifyOrderRequest request) {
        
        return modifyOrder(orderId, request);
    }
}