package com.ipayz.ipayz_backend.service;

import java.time.Duration;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    
    // -----------------------------
    // Brevo configuration
    // -----------------------------
    @Value("${brevo.api.base-url}")
    private String brevoBaseUrl; // Option A: field injection

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    // -----------------------------
    // Email sender info
    // -----------------------------
    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.fromName:}")
    private String fromName;

    @Value("${app.mail.replyTo:}")
    private String replyTo;

    @Value("${app.mail.admin}")
    private String adminMail;

    // -----------------------------
    // WebClient instance
    // -----------------------------
    private final WebClient webClient;

    // -----------------------------
    // Constructor
    // -----------------------------
    public EmailService(@Value("${brevo.api.base-url:https://api.brevo.com}") String brevoBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(brevoBaseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // =========================
    // OTP Email
    // =========================
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        String subject = "Your OTP Code for " + purpose;
        String htmlBody = buildOtpHtmlTemplate(otpCode, purpose);
        sendEmail(toEmail, subject, htmlBody);
    }

    // =========================
    // Generic email send
    // =========================
    @Async
    public void sendPlainTextEmail(String toEmail, String subject, String htmlBody) {
        sendEmail(toEmail, subject, htmlBody);
    }

    // =========================
    // Core: send via Brevo REST API
    // =========================
    @Async
    public void sendEmail(String to, String subject, String htmlBody) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            logger.error("Brevo API key not configured; email not sent to {}", to);
            return;
        }

        if (from == null || from.isBlank()) {
            logger.error("Sender address (app.mail.from) is not configured; email not sent to {}", to);
            return;
        }

        Map<String, Object> payload = new HashMap<>();

        Map<String, String> sender = new HashMap<>();
        sender.put("email", from);
        if (fromName != null && !fromName.isBlank()) {
            sender.put("name", fromName);
        }
        payload.put("sender", sender);

        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", to);
        payload.put("to", Collections.singletonList(recipient));

        payload.put("subject", subject);
        payload.put("htmlContent", htmlBody);

        if (replyTo != null && !replyTo.isBlank()) {
            Map<String, String> replyToObj = new HashMap<>();
            replyToObj.put("email", replyTo);
            payload.put("replyTo", replyToObj);
        }

        try {
            String response = webClient.post()
                    .uri("/v3/smtp/email") // relative to baseUrl
                    .header("api-key", brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(
                            status -> !status.is2xxSuccessful(),
                            clientResp -> clientResp.bodyToMono(String.class)
                                    .map(body -> new RuntimeException(
                                            "Brevo API error " + clientResp.statusCode() + " - " + body))
                    )
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(20));

            logger.info("Sent email to {} via Brevo; response: {}", to, response);

        } catch (Exception ex) {
            logger.error("Failed to send email via Brevo to {}: {}", to, ex.getMessage(), ex);
        }
    }

    // =========================
    // OTP template
    // =========================
    private String buildOtpHtmlTemplate(String otpCode, String purpose) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif; line-height: 1.6;\">"
                + "<h2 style=\"color:#2d89ef;\">iPay Security Verification</h2>"
                + "<p>Dear Customer,</p>"
                + "<p>Your OTP code for <strong>" + purpose + "</strong> is:</p>"
                + "<h1 style=\"color:#2d89ef; letter-spacing: 4px;\">" + otpCode + "</h1>"
                + "<p>This code will expire in 10 minutes.<br/>If you did not request this, please ignore this email.</p>"
                + "<br/>"
                + "<p>Best regards,<br/>The iPay Team</p>"
                + "</body>"
                + "</html>";
    }

    public String getAdminMail() {
        return adminMail;
    }
}
