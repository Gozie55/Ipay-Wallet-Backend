package com.ipayz.ipayz_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
public class TransactionEntity {

    public enum TransactionType {
        FUND, WALLET_TRANSFER, BANK_TRANSFER
    }

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletEntity wallet;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TransactionType type;

    @NotNull
    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TransactionStatus status;

    @NotNull
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String reference;

    // ✅ NEW FIELD: shared ID for linking related transactions (e.g. debit & credit)
    @Column(name = "transfer_group_id", length = 100)
    private String transferGroupId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    // Store metadata as String
    @Column(columnDefinition = "TEXT") // optional, ensures large text support
    private String metadata;

    private String externalResponse;

    public TransactionEntity() {
    }

    public TransactionEntity(WalletEntity wallet, BigDecimal amount, TransactionType type, TransactionStatus status, String reference) {
        this.wallet = wallet;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.reference = reference;
        this.timestamp = Instant.now();
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WalletEntity getWallet() {
        return wallet;
    }

    public void setWallet(WalletEntity wallet) {
        this.wallet = wallet;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getExternalResponse() {
        return externalResponse;
    }

    public void setExternalResponse(String externalResponse) {
        this.externalResponse = externalResponse;
    }

    // --- Convenience Methods ---
    public void markSuccess() {
        this.status = TransactionStatus.SUCCESS;
    }

    public void markFailed() {
        this.status = TransactionStatus.FAILED;
    }

    public void markPending() {
        this.status = TransactionStatus.PENDING;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getTransferGroupId() {
        return transferGroupId;
    }

    public void setTransferGroupId(String transferGroupId) {
        this.transferGroupId = transferGroupId;
    }
}
