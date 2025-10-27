package com.brokerx.repository;

import com.brokerx.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByAccount_AccountId(UUID accountId);
    Optional<Order> findByAccount_AccountIdAndClientOrderId(UUID accountId, String clientOrderId);
}