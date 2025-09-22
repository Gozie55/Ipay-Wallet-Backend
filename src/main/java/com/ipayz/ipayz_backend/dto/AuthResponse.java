package com.ipayz.ipayz_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private boolean isKycCompleted;
    private String accountNumber;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isIsKycCompleted() {
        return isKycCompleted;
    }

    public void setIsKycCompleted(boolean isKycCompleted) {
        this.isKycCompleted = isKycCompleted;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
