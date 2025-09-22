package com.ipayz.ipayz_backend.dto;

public class LoginVerifyResponse {
    private String token;         // Backend-issued JWT
    private String accountNumber; // User’s wallet/account number
    private String email;         // Optional: return for client display

    // === Constructors ===
    public LoginVerifyResponse() {
    }

    public LoginVerifyResponse(String token, String email) {
        this.token = token;
        this.email = email;
    }

    public LoginVerifyResponse(String token, String accountNumber, String email) {
        this.token = token;
        this.accountNumber = accountNumber;
        this.email = email;
    }

    // === Getters/Setters ===
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
