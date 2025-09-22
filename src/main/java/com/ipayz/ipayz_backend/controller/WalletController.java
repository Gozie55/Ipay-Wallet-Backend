package com.ipayz.ipayz_backend.controller;

import com.ipayz.ipayz_backend.dto.FundCardRequest;
import com.ipayz.ipayz_backend.service.WalletService;
import com.ipayz.ipayz_backend.service.WalletService.WalletBalanceDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@Validated
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // Get wallet balance
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@AuthenticationPrincipal String email) {
        WalletBalanceDto dto = walletService.getBalance(email);
        return ResponseEntity.ok(Map.of("success", true, "data", dto));
    }

    // Fund wallet with card (initiates Monnify card flow)
    @PostMapping("/fund/card")
    public ResponseEntity<?> fundWithCard(@AuthenticationPrincipal String email,
                                          @RequestBody FundCardRequest request) {
        Map<String, Object> response = walletService.fundWithCard(email, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    // Create reserved account for bank funding
    @PostMapping("/fund/bank")
    public ResponseEntity<?> createReservedAccount(@AuthenticationPrincipal String email) {
        Map<String, Object> response = walletService.createReservedAccount(email);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    // Get banks (delegated)
    @GetMapping("/banks")
    public ResponseEntity<?> getBanks(@AuthenticationPrincipal String email) {
        Object banks = walletService.getBanks();
        return ResponseEntity.ok(Map.of("success", true, "data", banks));
    }
}
