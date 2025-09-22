package com.ipayz.ipayz_backend.service;

import com.ipayz.ipayz_backend.dto.TransferBankRequest;
import com.ipayz.ipayz_backend.dto.TransferWalletRequest;
import com.ipayz.ipayz_backend.entity.TransactionEntity;
import com.ipayz.ipayz_backend.entity.UserEntity;
import com.ipayz.ipayz_backend.entity.WalletEntity;
import com.ipayz.ipayz_backend.repository.TransactionRepository;
import com.ipayz.ipayz_backend.repository.UserRepository;
import com.ipayz.ipayz_backend.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final MonnifyService monnifyService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // <-- added

    public TransactionService(WalletRepository walletRepository,
                              TransactionRepository transactionRepository,
                              MonnifyService monnifyService,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.monnifyService = monnifyService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Wallet -> Wallet transfer
     */
    @Transactional
    public Map<String, Object> transferBetweenWallets(String senderEmail, TransferWalletRequest request) {
        UserEntity senderUser = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender user not found"));

        WalletEntity sender = walletRepository.findFirstByUserId(senderUser.getId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

        WalletEntity recipient = walletRepository.findByAccountNumber(request.getRecipientAccountNumber())
                .orElseThrow(() -> new RuntimeException("Recipient wallet not found"));

        // ✅ verify PIN using PasswordEncoder
        if (senderUser.getWalletPinHash() == null ||
                !passwordEncoder.matches(request.getWalletPin(), senderUser.getWalletPinHash())) {
            throw new RuntimeException("Invalid wallet PIN");
        }

        // Lock both wallets to avoid race conditions
        WalletEntity lockedSender = walletRepository.findByIdForUpdate(sender.getId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found (locked)"));
        WalletEntity lockedRecipient = walletRepository.findByAccountNumberForUpdate(recipient.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Recipient wallet not found (locked)"));

        if (lockedSender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // debit sender
        lockedSender.setBalance(lockedSender.getBalance().subtract(request.getAmount()));
        lockedSender.setUpdatedAt(Instant.now());
        walletRepository.save(lockedSender);

        // credit recipient
        lockedRecipient.setBalance(lockedRecipient.getBalance().add(request.getAmount()));
        lockedRecipient.setUpdatedAt(Instant.now());
        walletRepository.save(lockedRecipient);

        String reference = "WALLET-" + UUID.randomUUID();
        TransactionEntity debitTxn = new TransactionEntity(
                lockedSender, request.getAmount().negate(),
                TransactionEntity.TransactionType.WALLET_TRANSFER,
                TransactionEntity.TransactionStatus.SUCCESS, reference);

        TransactionEntity creditTxn = new TransactionEntity(
                lockedRecipient, request.getAmount(),
                TransactionEntity.TransactionType.WALLET_TRANSFER,
                TransactionEntity.TransactionStatus.SUCCESS, reference);

        transactionRepository.save(debitTxn);
        transactionRepository.save(creditTxn);

        return Map.of(
                "status", "SUCCESS",
                "reference", reference,
                "senderNewBalance", lockedSender.getBalance()
        );
    }

    /**
     * Wallet -> Bank transfer (withdraw).
     */
    @Transactional
    public Map<String, Object> transferToBank(String senderEmail, TransferBankRequest request) {
        UserEntity senderUser = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender user not found"));

        WalletEntity sender = walletRepository.findFirstByUserId(senderUser.getId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

        // ✅ verify PIN using PasswordEncoder
        if (senderUser.getWalletPinHash() == null ||
                !passwordEncoder.matches(request.getWalletPin(), senderUser.getWalletPinHash())) {
            throw new RuntimeException("Invalid wallet PIN");
        }

        WalletEntity lockedSender = walletRepository.findByIdForUpdate(sender.getId())
                .orElseThrow(() -> new RuntimeException("Sender wallet not found (locked)"));

        if (lockedSender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        // debit immediately
        lockedSender.setBalance(lockedSender.getBalance().subtract(request.getAmount()));
        lockedSender.setUpdatedAt(Instant.now());
        walletRepository.save(lockedSender);

        String reference = "BANK-" + UUID.randomUUID();
        TransactionEntity txn = new TransactionEntity(
                lockedSender, request.getAmount().negate(),
                TransactionEntity.TransactionType.BANK_TRANSFER,
                TransactionEntity.TransactionStatus.PENDING, reference);
        txn.setTimestamp(Instant.now());
        transactionRepository.save(txn);

        // call Monnify
        Map<String, Object> monnifyResp = monnifyService.transferToBank(
                request.getAmount(), request.getAccountNumber(), request.getBankCode(), "Wallet withdrawal");

        txn.setExternalResponse(monnifyResp.toString());
        Object status = monnifyResp.get("status");
        if (status != null && status.toString().equalsIgnoreCase("SUCCESS")) {
            txn.setStatus(TransactionEntity.TransactionStatus.SUCCESS);
        } else {
            txn.setStatus(TransactionEntity.TransactionStatus.PENDING);
        }
        transactionRepository.save(txn);

        return Map.of(
                "status", txn.getStatus().name(),
                "reference", reference,
                "newBalance", lockedSender.getBalance()
        );
    }

    /**
     * Transaction history
     */
    public List<Map<String, Object>> getTransactionHistory(String email, int page, int size) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TransactionEntity> txns = transactionRepository.findByWallet_User_Id(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (TransactionEntity t : txns) {
            result.add(Map.of(
                    "reference", t.getReference(),
                    "type", t.getType().name(),
                    "amount", t.getAmount(),
                    "status", t.getStatus().name(),
                    "createdAt", t.getTimestamp(),
                    "metadata", t.getMetadata()
            ));
        }
        return result;
    }

    /**
     * Expose bank list via Monnify
     */
    public List<Map<String, Object>> getBanks() {
        return monnifyService.getBanks();
    }
}
