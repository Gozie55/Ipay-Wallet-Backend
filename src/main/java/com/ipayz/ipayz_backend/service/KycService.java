package com.ipayz.ipayz_backend.service;

import com.ipayz.ipayz_backend.dto.KycRequest;
import com.ipayz.ipayz_backend.dto.KycResponse;
import com.ipayz.ipayz_backend.entity.KycProfileEntity;
import com.ipayz.ipayz_backend.entity.UserEntity;
import com.ipayz.ipayz_backend.entity.WalletEntity;
import com.ipayz.ipayz_backend.repository.KycProfileRepository;
import com.ipayz.ipayz_backend.repository.UserRepository;
import com.ipayz.ipayz_backend.repository.WalletRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class KycService {

    private final UserRepository userRepository;
    private final KycProfileRepository kycProfileRepository;
    private final WalletRepository walletRepository;

    public KycService(UserRepository userRepository,
                      KycProfileRepository kycProfileRepository,
                      WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.kycProfileRepository = kycProfileRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional
    public KycResponse completeKyc(String email, KycRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save KYC profile
        KycProfileEntity kyc = new KycProfileEntity();
        kyc.setUser(user);
        kyc.setFirstName(request.getFirstName());
        kyc.setLastName(request.getLastName());
        kyc.setAddress(request.getAddress());
        kyc.setBvn(request.getBvn());
        user.setPhoneNumber(request.getPhoneNumber());
        kyc.setSelfieUrl(request.getSelfieUrl());
        kyc.setCreatedAt(Instant.now());
        kyc.setUpdatedAt(Instant.now());

        kycProfileRepository.save(kyc);

        // Generate account number from phone number (without the first zero)
        String accountNumber = request.getPhoneNumber().substring(1);

        // Create wallet
        WalletEntity wallet = new WalletEntity();
        wallet.setUser(user);
        wallet.setAccountNumber(accountNumber);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("NGN");
        wallet.setCreatedAt(Instant.now());

        walletRepository.save(wallet);

        user.setKycCompleted(true);
        userRepository.save(user);

        return new KycResponse(accountNumber, 0.00, "KYC completed, wallet created");
    }
}
