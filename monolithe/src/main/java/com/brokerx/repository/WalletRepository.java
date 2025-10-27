package com.brokerx.repository;

import com.brokerx.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByAccount_AccountId(UUID accountId);
}
