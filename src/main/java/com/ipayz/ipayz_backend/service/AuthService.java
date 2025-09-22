package com.ipayz.ipayz_backend.service;

import com.ipayz.ipayz_backend.dto.*;
import com.ipayz.ipayz_backend.entity.UserEntity;
import com.ipayz.ipayz_backend.repository.UserRepository;
import com.ipayz.ipayz_backend.config.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(OtpService otpService,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;

    }

    // === Registration with OTP ===
    public void initiateRegistration(RegistrationInitiateRequest request) {
        // OtpService will generate + send email
        otpService.createOtp(request.getEmail(), "registration");
    }

    public RegistrationVerifyResponse verifyRegistrationOtp(RegistrationVerifyRequest request) {
        boolean valid = otpService.validateOtp(request.getEmail(), request.getOtp(), "registration");
        if (!valid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Create new user if not already registered
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(request.getEmail());
                    newUser.setVerified(false);
                    return userRepository.save(newUser);
                });

        // ✅ Issue JWT
        String token = jwtUtil.generateToken(user.getEmail());

        // ✅ Return response with token + KYC status
        return new RegistrationVerifyResponse(token, user.getEmail(), "PENDING");
    }

    // === Login with OTP ===
    public void initiateLogin(LoginInitiateRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        // OtpService will generate + send email
        otpService.createOtp(request.getEmail(), "login");
    }

    public LoginVerifyResponse verifyLoginOtp(LoginVerifyRequest request) {
        boolean valid = otpService.validateOtp(request.getEmail(), request.getOtp(), "login");
        if (!valid) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail());

        // Get account number directly from user -> wallet
        String accountNumber = (user.getWallet() != null) ? user.getWallet().getAccountNumber() : null;

        return new LoginVerifyResponse(token, accountNumber, user.getEmail());
    }

    // === PIN Management ===
    public void setPin(String email, SetPinRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setWalletPinHash(passwordEncoder.encode(request.getWalletPin()));
        userRepository.save(user);
    }

    public void changePin(String email, ChangePinRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPin(), user.getWalletPinHash())) {
            throw new RuntimeException("Old PIN is incorrect");
        }
        user.setWalletPinHash(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);
    }
}
