package com.ipayz.ipayz_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class KycRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String address;

    @NotBlank(message = "BVN is required")
    @Size(min = 11, max = 11, message = "BVN must be 11 digits")
    private String bvn;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "0\\d{10}", message = "Phone number must start with 0 and be 11 digits")
    private String phoneNumber;

    @NotBlank(message = "Selfie URL is required")
    @Pattern(
        regexp = "^(http(s?):)([/|.|\\w|\\s|-])*\\.(?:jpg|jpeg|png)$",
        message = "Selfie URL must be a valid image URL (jpg, jpeg, png)"
    )
    private String selfieUrl;

    // --- Getters & Setters ---
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getBvn() {
        return bvn;
    }
    public void setBvn(String bvn) {
        this.bvn = bvn;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }
    public void setSelfieUrl(String selfieUrl) {
        this.selfieUrl = selfieUrl;
    }
}
