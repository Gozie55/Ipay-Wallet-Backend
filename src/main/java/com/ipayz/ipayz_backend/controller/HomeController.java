package com.ipayz.ipayz_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple Home controller that returns a friendly message for the root URL (/).
 * Keeps separation of concerns from AuthController.
 */
@RestController
public class HomeController {

    @GetMapping({"/", ""})
    public ResponseEntity<Map<String, String>> home() {
        return ResponseEntity.ok(Map.of(
                "service", "iPayz Backend",
                "message", "Welcome to iPay Wallet — API is running!",
                "note", "Auth endpoints live under /api/auth"
        ));
    }
}
