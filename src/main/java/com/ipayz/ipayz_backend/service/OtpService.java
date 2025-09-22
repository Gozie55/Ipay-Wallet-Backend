package com.ipayz.ipayz_backend.service;

import com.ipayz.ipayz_backend.entity.OtpEntity;
import com.ipayz.ipayz_backend.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${otp.expiry-minutes}")
    private long expiryMinutes;

    public OtpService(OtpRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    public String createOtp(String email, String purpose) {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        Instant expiry = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);

        OtpEntity otp = new OtpEntity();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setPurpose(purpose);
        otp.setExpiryTime(expiry);
        otp.setCreatedAt(Instant.now());

        otpRepository.save(otp);

        // Send email using EmailService templates
        emailService.sendOtpEmail(email, code, purpose);

        return code;
    }

    public boolean validateOtp(String email, String code, String purpose) {
        Optional<OtpEntity> otpOpt = otpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpEntity otp = otpOpt.get();
        if (!otp.getCode().equals(code)) {
            return false;
        }

        if (otp.getExpiryTime().isBefore(Instant.now())) {
            otpRepository.delete(otp);
            return false;
        }

        otpRepository.delete(otp); // consume OTP
        return true;
    }

    @Scheduled(fixedRate = 60000) // every 1 minute
    public void cleanupExpiredOtps() {
        otpRepository.deleteAllByExpiryTimeBefore(Instant.now());
    }
}
