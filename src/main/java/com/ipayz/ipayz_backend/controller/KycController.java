package com.ipayz.ipayz_backend.controller;

import com.ipayz.ipayz_backend.dto.KycRequest;
import com.ipayz.ipayz_backend.dto.KycResponse;
import com.ipayz.ipayz_backend.service.KycService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/register")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/kyc")
    public ResponseEntity<KycResponse> completeKyc(@Valid @RequestBody KycRequest request) {
        String email = getAuthenticatedEmail();
        KycResponse response = kycService.completeKyc(email, request);
        return ResponseEntity.ok(response);
    }

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Unauthorized");
        }
        return auth.getName();
    }
}
