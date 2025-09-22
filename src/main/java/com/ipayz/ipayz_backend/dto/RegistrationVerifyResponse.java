package com.ipayz.ipayz_backend.dto;

public class RegistrationVerifyResponse {
    private String token;
    private String email;
    private String kycStatus;

    public RegistrationVerifyResponse(String token, String email, String kycStatus) {
        this.token = token;
        this.email = email;
        this.kycStatus = kycStatus;
    }

    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public String getKycStatus() {
        return kycStatus;
    }
}
