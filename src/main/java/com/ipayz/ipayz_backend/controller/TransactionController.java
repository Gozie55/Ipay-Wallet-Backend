package com.ipayz.ipayz_backend.controller;

import com.ipayz.ipayz_backend.dto.TransferBankRequest;
import com.ipayz.ipayz_backend.dto.TransferWalletRequest;
import com.ipayz.ipayz_backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/transaction")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Wallet -> Wallet
    @PostMapping("/transfer/wallet")
    public ResponseEntity<?> transferWallet(@AuthenticationPrincipal String email,
                                            @RequestBody TransferWalletRequest request) {
        Map<String, Object> result = transactionService.transferBetweenWallets(email, request);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    // Get banks
    @GetMapping("/banks")
    public ResponseEntity<?> getBanks() {
        return ResponseEntity.ok(Map.of("success", true, "data", transactionService.getBanks()));
    }

    // Wallet -> Bank (withdraw)
    @PostMapping("/transfer/bank")
    public ResponseEntity<?> transferBank(@AuthenticationPrincipal String email,
                                          @RequestBody TransferBankRequest request) {
        Map<String, Object> result = transactionService.transferToBank(email, request);
        return ResponseEntity.ok(Map.of("success", true, "data", result));
    }

    // Transaction history
    @GetMapping("/history")
    public ResponseEntity<?> history(@AuthenticationPrincipal String email,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of("success", true, "data", transactionService.getTransactionHistory(email, page, size)));
    }
}
