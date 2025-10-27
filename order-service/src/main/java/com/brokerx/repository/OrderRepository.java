package com.brokerx.repository;

import com.brokerx.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByAccountId(UUID accountId);
    Optional<Order> findByAccountIdAndClientOrderId(UUID accountId, String clientOrderId);
}