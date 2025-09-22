package com.ipayz.ipayz_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePinRequest {
    @NotBlank
    @Size(min = 4, max = 6)
    private String oldPin;

    @NotBlank
    @Size(min = 4, max = 6)
    private String newPin;

    public String getOldPin() {
        return oldPin;
    }

    public void setOldPin(String oldPin) {
        this.oldPin = oldPin;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }
}
