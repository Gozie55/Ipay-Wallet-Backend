package com.ipayz.ipayz_backend.controller;

import com.ipayz.ipayz_backend.dto.*;
import com.ipayz.ipayz_backend.service.AuthService;
import com.ipayz.ipayz_backend.service.KycService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService, KycService kycService) {
        this.authService = authService;
    }

    // === Registration Initiate ===
    @PostMapping("/register/initiate")
    public ResponseEntity<?> initiateRegistration(@RequestBody RegistrationInitiateRequest request) {
        try {
            authService.initiateRegistration(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "OTP sent successfully to your email"
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred. Please try again."
            ));
        }
    }

    // === Registration Verify (Email + OTP) ===
    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyRegistration(@Valid @RequestBody RegistrationVerifyRequest request) {
        try {
            return ResponseEntity.ok(authService.verifyRegistrationOtp(request));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred while verifying registration."
            ));
        }
    }

    // === Set Wallet PIN ===
    @PostMapping("/set-pin")
    public ResponseEntity<?> setPin(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody SetPinRequest request) {
        try {
            authService.setPin(email, request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Wallet PIN set successfully"
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred while setting PIN."
            ));
        }
    }

    // === Change Wallet PIN ===
    @PostMapping("/change-pin")
    public ResponseEntity<?> changePin(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePinRequest request) {
        try {
            authService.changePin(email, request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Wallet PIN changed successfully"
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred while changing PIN."
            ));
        }
    }

    // === Login Initiate ===
    @PostMapping("/login/initiate")
    public ResponseEntity<?> initiateLogin(@Valid @RequestBody LoginInitiateRequest request) {
        try {
            authService.initiateLogin(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "OTP sent to your email"
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred during login initiation."
            ));
        }
    }

    // === Login Verify ===
    @PostMapping("/login/verify")
    public ResponseEntity<?> verifyLogin(@Valid @RequestBody LoginVerifyRequest request) {
        try {
            return ResponseEntity.ok(authService.verifyLoginOtp(request));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred during login verification."
            ));
        }
    }
}
