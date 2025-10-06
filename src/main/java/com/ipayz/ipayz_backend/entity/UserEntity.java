package com.ipayz.ipayz_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "account_number", unique = true, length = 20)
    private String accountNumber;

    @Column(name = "is_verified")
    private boolean isVerified = false;

    private boolean kycCompleted;

    @Size(max = 255)
    @Column(name = "wallet_pin_hash")
    private String walletPinHash;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private WalletEntity wallet;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private KycProfileEntity kycProfile;

    public UserEntity() {
    }

    // --- Getters & Setters ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isKycCompleted() {
        return kycCompleted;
    }

    public void setKycCompleted(boolean kycCompleted) {
        this.kycCompleted = kycCompleted;
    }

    public String getWalletPinHash() {
        return walletPinHash;
    }

    public void setWalletPinHash(String walletPinHash) {
        this.walletPinHash = walletPinHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public KycProfileEntity getKycProfile() {
        return kycProfile;
    }

    public void setKycProfile(KycProfileEntity kycProfile) {
        this.kycProfile = kycProfile;
    }

    public boolean isIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public WalletEntity getWallet() {
        return wallet;
    }

    public void setWallet(WalletEntity wallet) {
        this.wallet = wallet;
    }

    @Transient
    public String getFullName() {
        if (kycProfile != null) {
            String first = kycProfile.getFirstName() != null ? kycProfile.getFirstName() : "";
            String last = kycProfile.getLastName() != null ? kycProfile.getLastName() : "";
            return (first + " " + last).trim();
        }
        return email; // fallback if KYC is missing
    }

}
