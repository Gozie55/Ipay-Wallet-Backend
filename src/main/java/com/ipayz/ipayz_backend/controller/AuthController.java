package com.ipayz.ipayz_backend.controller;

import com.ipayz.ipayz_backend.service.AuthService;
import com.ipayz.ipayz_backend.service.KycService;
import com.ipayz.ipayz_backend.dto.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
  

    public AuthController(AuthService authService, KycService kycService) {
        this.authService = authService;
       
    }

    // === Registration Initiate (Email + OTP) ===
    @PostMapping("/register/initiate")
    public ResponseEntity<?> initiateRegistration(@Valid @RequestBody RegistrationInitiateRequest request) {
        System.out.println("[AuthController] initiateRegistration called with email: " + request.getEmail());
        authService.initiateRegistration(request);
        return ResponseEntity.ok().body(Map.of("message", "OTP sent to your email"));
    }

    // === Registration Verify (Email + OTP) ===
    @PostMapping("/register/verify")
    public ResponseEntity<RegistrationVerifyResponse> verifyRegistration(
            @Valid @RequestBody RegistrationVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyRegistrationOtp(request));
    }

    // === Set Wallet PIN ===
    @PostMapping("/set-pin")
    public ResponseEntity<?> setPin(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody SetPinRequest request) {
        authService.setPin(email, request);
        return ResponseEntity.ok(Map.of("message", "Wallet PIN set successfully"));
    }

    // === Change Wallet PIN ===
    @PostMapping("/change-pin")
    public ResponseEntity<?> changePin(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePinRequest request) {
        authService.changePin(email, request);
        return ResponseEntity.ok(Map.of("message", "Wallet PIN changed successfully"));
    }

    // === Login Initiate ===
    @PostMapping("/login/initiate")
    public ResponseEntity<?> initiateLogin(@Valid @RequestBody LoginInitiateRequest request) {
        authService.initiateLogin(request);
        return ResponseEntity.ok(Map.of("message", "OTP sent to email"));
    }

    // === Login Verify ===
    @PostMapping("/login/verify")
    public ResponseEntity<LoginVerifyResponse> verifyLogin(@Valid @RequestBody LoginVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request));
    }
}
