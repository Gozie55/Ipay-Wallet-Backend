package com.ipayz.ipayz_backend.service;

import com.ipayz.ipayz_backend.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MonnifyService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${monnify.base-url}")
    private String baseUrl;

    @Value("${monnify.api-key}")
    private String apiKey;

    @Value("${monnify.secret-key}")
    private String secretKey;

    @Value("${monnify.contract-code}")
    private String contractCode;

    @Value("${monnify.retry-count:3}")
    private int retryCount;

    private volatile String cachedToken;
    private volatile long tokenExpiry = 0;

    /**
     * Initialize a card payment with Monnify.
     * Returns the Monnify response body (responseBody) as a Map.
     * Expected keys include paymentReference and paymentUrl (depends on Monnify response).
     */
    public Map<String, Object> initiateCardPayment(UserEntity user, BigDecimal amount) {
        String token = getAccessToken();
        String url = baseUrl + "/api/v1/merchant/transactions/init-transaction";

        String customerName = user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()
                ? user.getPhoneNumber()
                : user.getEmail();

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount.doubleValue());
        body.put("customerEmail", user.getEmail());
        body.put("customerName", customerName);
        body.put("paymentReference", "CARD-" + System.currentTimeMillis());
        body.put("paymentDescription", "iPayz Wallet Funding");
        body.put("currencyCode", "NGN");
        body.put("contractCode", contractCode);
        // redirectUrl should be your frontend or backend callback that completes the flow
        body.put("redirectUrl", "https://ipayz.com/payment/callback");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to initialize Monnify card payment");
        }

        // Many Monnify endpoints place the useful info under "responseBody"
        Map<String, Object> resp = (Map<String, Object>) response.getBody().get("responseBody");
        if (resp == null) {
            // fallback to entire body
            resp = response.getBody();
        }
        return resp;
    }

    /**
     * Create a reserved (virtual) account for bank funding.
     * Returns Monnify response body as Map. Useful keys: accountReference, accountNumber, bankName.
     */
    public Map<String, Object> createReservedAccount(UserEntity user) {
        String token = getAccessToken();
        String url = baseUrl + "/api/v2/bank-transfer/reserved-accounts";

        String customerName = user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()
                ? user.getPhoneNumber()
                : user.getEmail();

        Map<String, Object> body = new HashMap<>();
        body.put("accountReference", "RES-" + user.getId() + "-" + System.currentTimeMillis());
        body.put("accountName", "iPayz Wallet - " + customerName);
        body.put("currencyCode", "NGN");
        body.put("contractCode", contractCode);
        body.put("customerEmail", user.getEmail());
        body.put("customerName", customerName);
        // If Monnify supports getAllAvailableBanks flag
        body.put("getAllAvailableBanks", true);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to create reserved account with Monnify");
        }

        Map<String, Object> resp = (Map<String, Object>) response.getBody().get("responseBody");
        if (resp == null) {
            resp = response.getBody();
        }
        return resp;
    }

    /**
     * Disburse (transfer) to an external bank account.
     * Returns a result map with status, reference and raw response
     */
    public Map<String, Object> transferToBank(BigDecimal amount, String accountNumber, String bankCode, String narration) {
        String token = getAccessToken();
        String url = baseUrl + "/api/v1/disbursements/single";

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount.doubleValue());
        String reference = "MN-" + System.currentTimeMillis();
        body.put("reference", reference);
        body.put("narration", narration);
        body.put("destinationBankCode", bankCode);
        body.put("destinationAccountNumber", accountNumber);
        body.put("currency", "NGN");
        body.put("contractCode", contractCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        int attempts = 0;
        while (attempts < retryCount) {
            try {
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
                Map<String, Object> result = new HashMap<>();
                result.put("status", response.getStatusCode().is2xxSuccessful() ? "SUCCESS" : "FAILED");
                result.put("reference", reference);
                result.put("response", response.getBody());
                return result;
            } catch (Exception e) {
                attempts++;
                if (attempts >= retryCount) {
                    throw new RuntimeException("Monnify transfer failed after retries", e);
                }
                try { Thread.sleep((long) Math.pow(2, attempts) * 1000); } catch (InterruptedException ignored) {}
            }
        }
        throw new RuntimeException("Monnify transfer failed unexpectedly");
    }

    /**
     * Get supported bank list from Monnify
     */
    public List<Map<String, Object>> getBanks() {
        String token = getAccessToken();
        String url = baseUrl + "/api/v1/banks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to fetch banks from Monnify");
        }

        Map<String, Object> body = response.getBody();
        return (List<Map<String, Object>>) body.get("responseBody");
    }

    /**
     * Obtain and cache Monnify access token
     */
    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < tokenExpiry) return cachedToken;

        String url = baseUrl + "/api/v1/auth/login";
        String authString = apiKey + ":" + secretKey;
        String encodedAuth = Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to obtain Monnify access token");
        }

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody().get("responseBody");

        cachedToken = (String) responseBody.get("accessToken");
        int expiresIn = (Integer) responseBody.get("expiresIn");
        tokenExpiry = now + (expiresIn - 60) * 1000L;

        return cachedToken;
    }
}
