package com.ipayz.ipayz_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.fromName:}")
    private String fromName;

    @Value("${app.mail.replyTo:}")
    private String replyTo;

    @Value("${app.mail.admin}")
    private String adminMail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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
    // Plain Text Email
    // =========================
    @Async
    public void sendPlainTextEmail(String toEmail, String subject, String htmlBody) {
        sendEmail(toEmail, subject, htmlBody);
    }

    // =========================
    // Core Email Sending
    // =========================
    @Async
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            String sender = (fromName != null && !fromName.isBlank()) 
                    ? String.format("%s <%s>", fromName, from) 
                    : from;

            helper.setFrom(new InternetAddress(sender));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email via Gmail SMTP: " + e.getMessage(), e);
        }
    }

    // =========================
    // OTP Email Template
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
