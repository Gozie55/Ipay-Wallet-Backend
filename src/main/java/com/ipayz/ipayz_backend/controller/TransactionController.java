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

    // === Wallet → Wallet Transfer ===
    @PostMapping("/transfer/wallet")
    public ResponseEntity<?> transferWallet(@AuthenticationPrincipal String email,
            @RequestBody TransferWalletRequest request) {
        try {
            Map<String, Object> result = transactionService.transferBetweenWallets(email, request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", result
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred while processing wallet transfer."
            ));
        }
    }

    // === Wallet → Bank Transfer (Withdraw) ===
    @PostMapping("/transfer/bank")
    public ResponseEntity<?> transferBank(@AuthenticationPrincipal String email,
            @RequestBody TransferBankRequest request) {
        try {
            Map<String, Object> result = transactionService.transferToBank(email, request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", result
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred while processing bank transfer."
            ));
        }
    }

    // === Bank List ===
    @GetMapping("/banks")
    public ResponseEntity<?> getBanks() {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", transactionService.getBanks()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Unable to fetch banks list at this time."
            ));
        }
    }

    // === Transaction History ===
    @GetMapping("/history")
    public ResponseEntity<?> history(@AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", transactionService.getTransactionHistory(email, page, size)
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", ex.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred while fetching transaction history."
            ));
        }
    }
}
