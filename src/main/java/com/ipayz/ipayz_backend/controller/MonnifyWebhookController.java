package com.ipayz.ipayz_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipayz.ipayz_backend.entity.TransactionEntity;
import com.ipayz.ipayz_backend.entity.UserEntity;
import com.ipayz.ipayz_backend.entity.WalletEntity;
import com.ipayz.ipayz_backend.repository.TransactionRepository;
import com.ipayz.ipayz_backend.repository.UserRepository;
import com.ipayz.ipayz_backend.repository.WalletRepository;
import com.ipayz.ipayz_backend.service.EmailService;
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
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${monnify.secret-key}")
    private String monnifySecretKey;

    public MonnifyWebhookController(TransactionRepository transactionRepository,
            WalletRepository walletRepository,
            UserRepository userRepository,
            EmailService emailService,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @PostMapping
    public ResponseEntity<?> handleWebhook(HttpServletRequest request) {
        try {
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            String receivedSignature = request.getHeader("monnify-signature");

            if (receivedSignature == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Missing signature"));
            }

            String expectedSignature = computeHmacSHA512(rawBody, monnifySecretKey);
            if (!expectedSignature.equalsIgnoreCase(receivedSignature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid signature"));
            }

            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            log.info("Valid Monnify webhook received: {}", payload);

            String transactionReference = (String) payload.get("transactionReference");
            String paymentReference = (String) payload.get("paymentReference");
            String paymentStatus = (String) payload.getOrDefault("paymentStatus", payload.get("status"));
            String customerEmail = (String) payload.getOrDefault("customerEmail", payload.get("customer_email"));
            Object amountObj = payload.get("amount");

            Number amountNumber = amountObj instanceof Number ? (Number) amountObj
                    : amountObj instanceof String ? Double.parseDouble((String) amountObj) : null;

            if ((transactionReference == null && paymentReference == null) || paymentStatus == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing reference or status"));
            }

            String finalStatus = paymentStatus.toUpperCase();
            String reference = paymentReference != null ? paymentReference : transactionReference;

            Optional<TransactionEntity> optTxn
                    = transactionRepository.findByExternalReference(transactionReference)
                            .or(() -> transactionRepository.findByReference(paymentReference));

            if (optTxn.isPresent()) {
                TransactionEntity txn = optTxn.get();
                txn.setStatus(TransactionEntity.TransactionStatus.valueOf(finalStatus));
                txn.setExternalResponse(payload.toString());
                transactionRepository.save(txn);

                if (txn.getStatus() == TransactionEntity.TransactionStatus.SUCCESS) {
                    WalletEntity wallet = walletRepository.findByIdForUpdate(txn.getWallet().getId())
                            .orElseThrow(() -> new RuntimeException("Wallet not found"));

                    wallet.setBalance(wallet.getBalance().add(txn.getAmount()));
                    wallet.setUpdatedAt(Instant.now());
                    walletRepository.save(wallet);

                    UserEntity user = wallet.getUser();

                    // ✅ Send email
                    emailService.sendWalletFundingReceipt(
                            user.getEmail(),
                            user.getFullName(),
                            wallet.getAccountNumber(),
                            txn.getAmount().toPlainString(),
                            txn.getReference()
                    );

                }

            } else {
                // ✅ Fallback
                if (customerEmail != null && amountNumber != null) {
                    userRepository.findByEmail(customerEmail).ifPresent(user -> {
                        walletRepository.findFirstByUserId(user.getId()).ifPresent(wallet -> {
                            TransactionEntity txn = new TransactionEntity();
                            txn.setWallet(wallet);
                            txn.setType(TransactionEntity.TransactionType.FUND);
                            txn.setAmount(BigDecimal.valueOf(amountNumber.doubleValue()));
                            txn.setStatus(TransactionEntity.TransactionStatus.valueOf(finalStatus));
                            txn.setReference(reference);
                            txn.setExternalResponse(payload.toString());
                            txn.setTimestamp(Instant.now());
                            transactionRepository.save(txn);

                            if (txn.getStatus() == TransactionEntity.TransactionStatus.SUCCESS) {
                                wallet.setBalance(wallet.getBalance().add(txn.getAmount()));
                                wallet.setUpdatedAt(Instant.now());
                                walletRepository.save(wallet);

                                // ✅ Send email after credit (fallback)
                                emailService.sendWalletFundingReceipt(
                                        user.getEmail(),
                                        user.getFullName(),
                                        wallet.getAccountNumber(),
                                        txn.getAmount().toPlainString(),
                                        txn.getReference()
                                );
                            }

                        });
                    });
                }
            }

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            log.error("Error processing Monnify webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Webhook processing failed"));
        }
    }

    private String computeHmacSHA512(String data, String secret) throws Exception {
        Mac sha512Hmac = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        sha512Hmac.init(keySpec);
        return Hex.encodeHexString(sha512Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
