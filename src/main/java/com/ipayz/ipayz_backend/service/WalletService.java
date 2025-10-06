package com.ipayz.ipayz_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipayz.ipayz_backend.dto.FundCardRequest;
import com.ipayz.ipayz_backend.entity.TransactionEntity;
import com.ipayz.ipayz_backend.entity.UserEntity;
import com.ipayz.ipayz_backend.entity.WalletEntity;
import com.ipayz.ipayz_backend.repository.TransactionRepository;
import com.ipayz.ipayz_backend.repository.UserRepository;
import com.ipayz.ipayz_backend.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final MonnifyService monnifyService;
    private final EmailService emailService;

    public WalletService(UserRepository userRepository,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            MonnifyService monnifyService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.monnifyService = monnifyService;
        this.emailService = emailService;
    }

    /**
     * Get wallet balance by user email (principal)
     */
    public WalletBalanceDto getBalance(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        WalletEntity wallet = walletRepository.findFirstByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + email));

        return new WalletBalanceDto(wallet.getAccountNumber(), wallet.getBalance());
    }

    /**
     * Initiate card funding flow: - Call Monnify to init card payment - Create
     * a PENDING TransactionEntity linked to the user's wallet using the payment
     * reference - Return Monnify response + stored reference
     */
    @Transactional
    public Map<String, Object> fundWithCard(String email, FundCardRequest request) {
        // Fetch user
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // Fetch wallet
        WalletEntity wallet = walletRepository.findFirstByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Call Monnify API
        Map<String, Object> monnifyResp = monnifyService.initiateCardPayment(user, request.getAmount());

        // Extract payment reference
        String paymentReference = (String) monnifyResp.getOrDefault(
                "paymentReference",
                monnifyResp.getOrDefault(
                        "paymentReferenceId",
                        monnifyResp.getOrDefault("reference", "CARD-" + System.currentTimeMillis())
                )
        );

        // Extract Monnify transaction reference (if present)
        String transactionReference = (String) monnifyResp.get("transactionReference");

        // ✅ Create pending transaction and save both refs
        TransactionEntity txn = new TransactionEntity();
        txn.setWallet(wallet);
        txn.setType(TransactionEntity.TransactionType.FUND);
        txn.setAmount(request.getAmount());
        txn.setStatus(TransactionEntity.TransactionStatus.PENDING);
        txn.setReference(paymentReference); // your internal reference
        txn.setExternalReference(transactionReference); // Monnify reference
        txn.setTimestamp(Instant.now());
        txn.setMetadata(monnifyResp.toString());

        // Save transaction
        transactionRepository.save(txn);

        // Prepare response for client
        Map<String, Object> result = new HashMap<>();
        result.put("monnify", monnifyResp);
        result.put("reference", paymentReference);

        return result;
    }

    /**
     * Create reserved account for bank funding (virtual account). No
     * transaction is created here — we create transactions only when payment is
     * confirmed.
     */
    public Map<String, Object> createReservedAccount(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Map<String, Object> resp = monnifyService.createReservedAccount(user);
        return resp;
    }

    /**
     * Expose Monnify banks (thin delegate)
     */
    public Object getBanks() {
        return monnifyService.getBanks();
    }

    // DTO used by controller
    public static class WalletBalanceDto {

        private final String accountNumber;
        private final BigDecimal balance;

        public WalletBalanceDto(String accountNumber, BigDecimal balance) {
            this.accountNumber = accountNumber;
            this.balance = balance;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public BigDecimal getBalance() {
            return balance;
        }
    }
}
