package com.ipayz.ipayz_backend.dto;

public class KycResponse {

    private String accountNumber;
    private Double walletBalance;
    private String message;

    public KycResponse(String accountNumber, Double walletBalance, String message) {
        this.accountNumber = accountNumber;
        this.walletBalance = walletBalance;
        this.message = message;
    }

    // --- Getters & Setters ---
    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Double getWalletBalance() {
        return walletBalance;
    }
    public void setWalletBalance(Double walletBalance) {
        this.walletBalance = walletBalance;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
