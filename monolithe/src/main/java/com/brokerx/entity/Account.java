package com.brokerx.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.brokerx.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Check(constraints = "status IN ('PENDING', 'ACTIVE', 'REJECTED')")
public class Account {

    @Id
    @GeneratedValue
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Account() {}

    // Getters / setters
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        if (!status.equals("PENDING") && !status.equals("ACTIVE") && !status.equals("REJECTED")) {
            throw new IllegalArgumentException("Invalid status");
        }
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
