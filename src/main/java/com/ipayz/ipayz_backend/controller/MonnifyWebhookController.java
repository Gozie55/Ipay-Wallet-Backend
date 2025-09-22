package com.ipayz.ipayz_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipayz.ipayz_backend.entity.TransactionEntity;
import com.ipayz.ipayz_backend.entity.WalletEntity;
import com.ipayz.ipayz_backend.repository.TransactionRepository;
import com.ipayz.ipayz_backend.repository.UserRepository;
import com.ipayz.ipayz_backend.repository.WalletRepository;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook/monnify")
public class MonnifyWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MonnifyWebhookController.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${monnify.secret-key}")
    private String monnifySecretKey;

    public MonnifyWebhookController(TransactionRepository transactionRepository,
                                    WalletRepository walletRepository,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @PostMapping
    public ResponseEntity<?> handleWebhook(HttpServletRequest request) {
        try {
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            String receivedSignature = request.getHeader("monnify-signature");

            if (receivedSignature == null) {
                log.warn("Webhook rejected: missing signature header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing signature"));
            }

            String expectedSignature = computeHmacSHA512(rawBody, monnifySecretKey);
            if (!expectedSignature.equalsIgnoreCase(receivedSignature)) {
                log.warn("Invalid Monnify webhook signature. expected={}, received={}", expectedSignature, receivedSignature);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid signature"));
            }

            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            log.info("Valid Monnify webhook received: {}", payload);

            // Extract common Monnify fields
            String transactionReference = (String) payload.getOrDefault("transactionReference", payload.get("paymentReference"));
            String paymentStatus = (String) payload.getOrDefault("paymentStatus", payload.get("status"));
            Object amountObj = payload.get("amount");

            Number amountNumber = null;
            if (amountObj instanceof Number) {
                amountNumber = (Number) amountObj;
            } else if (amountObj instanceof String) {
                try {
                    amountNumber = Double.parseDouble((String) amountObj);
                } catch (Exception ignored) {}
            }

            String customerEmail = (String) payload.getOrDefault("customerEmail", payload.get("customer_email"));

            if (transactionReference == null || paymentStatus == null) {
                log.warn("Webhook missing reference or status: {}", payload);
                return ResponseEntity.badRequest().body(Map.of("error", "Missing fields"));
            }

            // ✅ mark variables as final for lambda
            final String finalReference = transactionReference;
            final String finalStatus = paymentStatus.toUpperCase();
            final Number finalAmountNumber = amountNumber;
            final String finalCustomerEmail = customerEmail;
            final Map<String, Object> finalPayload = payload;

            Optional<TransactionEntity> optTxn = transactionRepository.findByReference(finalReference);

            if (optTxn.isPresent()) {
                // Update existing transaction
                TransactionEntity txn = optTxn.get();
                txn.setStatus(TransactionEntity.TransactionStatus.valueOf(finalStatus));
                txn.setExternalResponse(finalPayload.toString());
                transactionRepository.save(txn);
                log.info("Transaction {} updated to {}", finalReference, finalStatus);

                if (txn.getStatus() == TransactionEntity.TransactionStatus.SUCCESS) {
                    WalletEntity wallet = walletRepository.findByIdForUpdate(txn.getWallet().getId())
                            .orElseThrow(() -> new RuntimeException("Wallet not found for transaction"));

                    BigDecimal creditAmount = txn.getAmount();
                    wallet.setBalance(wallet.getBalance().add(creditAmount));
                    wallet.setUpdatedAt(Instant.now());
                    walletRepository.save(wallet);

                    txn.setStatus(TransactionEntity.TransactionStatus.SUCCESS);
                    transactionRepository.save(txn);

                    log.info("Credited wallet {} with {} for txn {}", wallet.getAccountNumber(), creditAmount, finalReference);
                }
            } else {
                // Fallback: no existing txn, try using email + amount
                log.info("Transaction not found for reference {}, attempting fallback by customerEmail", finalReference);
                if (finalCustomerEmail != null && finalAmountNumber != null) {
                    userRepository.findByEmail(finalCustomerEmail).ifPresent(user -> {
                        walletRepository.findFirstByUserId(user.getId()).ifPresentOrElse(wallet -> {
                            TransactionEntity txn = new TransactionEntity();
                            txn.setWallet(wallet);
                            txn.setType(TransactionEntity.TransactionType.FUND);
                            txn.setAmount(BigDecimal.valueOf(finalAmountNumber.doubleValue()));
                            txn.setStatus(TransactionEntity.TransactionStatus.valueOf(finalStatus));
                            txn.setReference(finalReference);
                            txn.setTimestamp(Instant.now());
                            txn.setExternalResponse(finalPayload.toString());
                            transactionRepository.save(txn);

                            if (txn.getStatus() == TransactionEntity.TransactionStatus.SUCCESS) {
                                WalletEntity lockedWallet = walletRepository.findByIdForUpdate(wallet.getId()).orElseThrow();
                                lockedWallet.setBalance(lockedWallet.getBalance().add(txn.getAmount()));
                                lockedWallet.setUpdatedAt(Instant.now());
                                walletRepository.save(lockedWallet);
                                log.info("Fallback: credited wallet {} (user {}) with {}", lockedWallet.getAccountNumber(), user.getEmail(), txn.getAmount());
                            }
                        }, () -> log.warn("Fallback: user {} has no wallet", user.getEmail()));
                    });
                } else {
                    log.warn("Fallback not possible - missing customerEmail or amount in payload");
                }
            }

            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (Exception e) {
            log.error("Error processing Monnify webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Webhook processing failed"));
        }
    }

    private String computeHmacSHA512(String data, String secret) throws Exception {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(keySpec);
        return Hex.encodeHexString(sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
