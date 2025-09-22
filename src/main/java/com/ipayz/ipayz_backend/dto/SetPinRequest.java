package com.ipayz.ipayz_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SetPinRequest {

    @NotBlank
    @Size(min = 4, max = 6)
    private String walletPin;

    public SetPinRequest() {
    }

    public String getWalletPin() {
        return walletPin;
    }

    public void setWalletPin(String walletPin) {
        this.walletPin = walletPin;
    }
}
