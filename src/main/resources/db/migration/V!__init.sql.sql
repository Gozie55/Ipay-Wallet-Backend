/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  CHIGOZIE
 * Created: 15 Sept 2025
 */

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    google_id VARCHAR(255),
    phone_number VARCHAR(20),
    account_number VARCHAR(20) UNIQUE,
    is_verified BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255),
    wallet_pin_hash VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Wallets table
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance DECIMAL(18,2) DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_wallet_user_id ON wallets(user_id);

-- Transactions table
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('FUND', 'TRANSFER', 'WITHDRAW')),
    amount DECIMAL(18,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    reference VARCHAR(100) UNIQUE NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    CONSTRAINT fk_transaction_wallet FOREIGN KEY (wallet_id)
        REFERENCES wallets (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_transaction_wallet_id ON transactions(wallet_id);
CREATE INDEX idx_transaction_status ON transactions(status);

-- Otps table
CREATE TABLE otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(20) NOT NULL,
    expiry_time TIMESTAMPTZ NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_email ON otps(email);
CREATE INDEX idx_otp_expiry_time ON otps(expiry_time);

-- KYC Profiles table
CREATE TABLE kyc_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    address TEXT,
    bvn VARCHAR(11) NOT NULL,
    selfie_url VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kyc_user FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_kyc_user_id ON kyc_profiles(user_id);
CREATE INDEX idx_kyc_bvn ON kyc_profiles(bvn);
