package com.ipayz.ipayz_backend.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // -----------------------------
    // SendGrid configuration
    // -----------------------------
    @Value("${sendgrid.api.key}")
    private String sendgridApiKey;

    @Value("${sendgrid.sender.email}")
    private String senderEmail;

    @Value("${sendgrid.sender.name:}")
    private String senderName;

    @Value("${app.mail.replyTo:}")
    private String replyTo;

    @Value("${app.mail.admin}")
    private String adminMail;

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
    // Core: send via SendGrid API
    // =========================
    @Async
    public void sendEmail(String to, String subject, String htmlBody) {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
            logger.error("SendGrid API key not configured; email not sent to {}", to);
            return;
        }

        if (senderEmail == null || senderEmail.isBlank()) {
            logger.error("Sender address (sendgrid.sender.email) is not configured; email not sent to {}", to);
            return;
        }

        Email from = new Email(senderEmail);
        if (senderName != null && !senderName.isBlank()) {
            from.setName(senderName);
        }

        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlBody);
        Mail mail = new Mail(from, subject, toEmail, content);

        if (replyTo != null && !replyTo.isBlank()) {
            Email replyToEmail = new Email(replyTo);
            mail.setReplyTo(replyToEmail);
        }

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            logger.info("Sent email to {} via SendGrid; status: {}, body: {}",
                    to, response.getStatusCode(), response.getBody());

        } catch (IOException ex) {
            logger.error("Failed to send email via SendGrid to {}: {}", to, ex.getMessage(), ex);
        }
    }

    // =========================
    // OTP template
    // =========================
    private String buildOtpHtmlTemplate(String otpCode, String purpose) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif; line-height: 1.6;\">"
                + "<h2 style=\"color:#2d89ef;\">iPay Account Verification</h2>"
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
