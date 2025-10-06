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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

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
    // OTP EMAIL
    // =========================
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        String subject = "🔐 Your OTP Code for " + purpose;
        String htmlBody = buildOtpHtmlTemplate(otpCode, purpose);
        sendEmail(toEmail, subject, htmlBody);
    }

    // =========================
    // GENERIC EMAIL
    // =========================
    @Async
    public void sendPlainTextEmail(String toEmail, String subject, String htmlBody) {
        sendEmail(toEmail, subject, htmlBody);
    }

    // =========================
    // CORE: Send via SendGrid
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

            logger.info("✅ Sent email to {} via SendGrid; status: {}, body: {}",
                    to, response.getStatusCode(), response.getBody());

        } catch (IOException ex) {
            logger.error("❌ Failed to send email via SendGrid to {}: {}", to, ex.getMessage(), ex);
        }
    }

    // =========================
    // HTML TEMPLATES
    // =========================
    private String buildOtpHtmlTemplate(String otpCode, String purpose) {
        String formattedTime = ZonedDateTime.now(ZoneId.of("Africa/Lagos"))
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, hh:mm a"));

        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif; line-height: 1.6; background-color:#f9fafb; padding:20px;\">"
                + "<div style=\"max-width:600px; margin:auto; background:#fff; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1); padding:20px;\">"
                + "<h2 style=\"color:#2d89ef; border-bottom:2px solid #2d89ef; padding-bottom:10px;\">iPay Account Verification</h2>"
                + "<p style=\"font-size:16px; color:#333;\">Your OTP for <strong>" + purpose + "</strong> is:</p>"
                + "<h1 style=\"color:#2d89ef; letter-spacing: 4px; text-align:center;\">" + otpCode + "</h1>"
                + "<p>This code expires in 10 minutes. If you did not request this, please ignore this email.</p>"
                + "<p style=\"font-size:13px; color:#777; margin-top:20px;\">🕓 " + formattedTime + " (West Africa Time)</p>"
                + "<br/>"
                + "<p style=\"font-size:15px; color:#444;\">Best regards,<br/><strong>The iPay Team</strong></p>"
                + "</div></body></html>";
    }

    private String buildTransactionHtml(String title, String message, String reference) {
        String formattedTime = ZonedDateTime.now(ZoneId.of("Africa/Lagos"))
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, hh:mm a"));

        return "<html><body style=\"font-family: Arial, sans-serif; line-height: 1.6; background-color:#f9fafb; padding:20px;\">"
                + "<div style=\"max-width:600px; margin:auto; background:#fff; border-radius:10px; box-shadow:0 2px 8px rgba(0,0,0,0.1); padding:20px;\">"
                + "<h2 style=\"color:#2d89ef; border-bottom:2px solid #2d89ef; padding-bottom:10px;\">" + title + "</h2>"
                + "<p style=\"font-size:16px; color:#333;\">" + message + "</p>"
                + "<p style=\"margin-top:15px;\"><strong>Reference:</strong> " + reference + "</p>"
                + "<p style=\"font-size:13px; color:#777; margin-top:20px;\">🕓 " + formattedTime + " (West Africa Time)</p>"
                + "<br/><p style=\"font-size:15px; color:#444;\">Thank you for using <strong>iPay</strong>.<br/>"
                + "Best regards,<br/><strong>The iPay Team</strong></p>"
                + "</div></body></html>";
    }

    // =========================
    // TRANSACTION EMAILS
    // =========================
    @Async
    public void sendWalletTransferEmail(
            String senderEmail,
            String recipientEmail,
            String amount,
            String reference,
            String senderName,
            String senderAccountNumber,
            String recipientName,
            String recipientAccountNumber
    ) {
        String subjectSender = "💸 Wallet Transfer Sent – ₦" + amount;
        String htmlSender = buildTransactionHtml(
                "Wallet Transfer Sent",
                "You have successfully transferred <strong>₦" + amount
                + "</strong> to <strong>" + recipientName + " (" + recipientAccountNumber + ")</strong>.",
                reference
        );

        String subjectRecipient = "💰 Wallet Credit Received – ₦" + amount;
        String htmlRecipient = buildTransactionHtml(
                "Wallet Transfer Received",
                "₦" + amount + " has been credited to your iPay wallet from <strong>"
                + senderName + " (" + senderAccountNumber + ")</strong>.",
                reference
        );

        sendEmail(senderEmail, subjectSender, htmlSender);
        sendEmail(recipientEmail, subjectRecipient, htmlRecipient);
    }

    @Async
    public void sendFundEmail(String toEmail, String amount, String reference) {
        String subject = "💳 Wallet Funded Successfully – ₦" + amount;
        String html = buildTransactionHtml(
                "Wallet Funding Successful",
                "Your iPay wallet has been funded with <strong>₦" + amount + "</strong>.",
                reference
        );
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendBankTransferEmail(String toEmail, String amount, String reference, String bankName, String accountNumber) {
        String subject = "🏦 Bank Transfer Initiated – ₦" + amount;
        String html = buildTransactionHtml(
                "Bank Transfer Initiated",
                "Your withdrawal of <strong>₦" + amount + "</strong> to <strong>" + bankName
                + " (" + accountNumber + ")</strong> has been initiated.",
                reference
        );
        sendEmail(toEmail, subject, html);
    }

    @Async
    public void sendWalletFundingReceipt(
            String toEmail,
            String userFullName,
            String walletAccountNumber,
            String amount,
            String reference
    ) {
        String subject = "💳 Wallet Funded Successfully – ₦" + amount;

        String formattedTime = ZonedDateTime.now(ZoneId.of("Africa/Lagos"))
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy, hh:mm a"));

        String htmlBody = "<html><body style=\"font-family:Arial,sans-serif;background-color:#f9fafb;padding:20px;\">"
                + "<div style=\"max-width:600px;margin:auto;background:#fff;border-radius:10px;"
                + "box-shadow:0 2px 8px rgba(0,0,0,0.1);padding:25px;\">"
                + "<h2 style=\"color:#2d89ef;border-bottom:2px solid #2d89ef;padding-bottom:10px;\">Wallet Funding Receipt</h2>"
                + "<p style=\"font-size:16px;color:#333;\">Hello <strong>" + userFullName + "</strong>,</p>"
                + "<p>Your wallet has been successfully funded with:</p>"
                + "<table style=\"width:100%;margin-top:10px;border-collapse:collapse;\">"
                + "<tr><td style='padding:8px;font-weight:bold;'>Amount:</td><td>₦" + amount + "</td></tr>"
                + "<tr><td style='padding:8px;font-weight:bold;'>Wallet Number:</td><td>" + walletAccountNumber + "</td></tr>"
                + "<tr><td style='padding:8px;font-weight:bold;'>Transaction Reference:</td><td>" + reference + "</td></tr>"
                + "<tr><td style='padding:8px;font-weight:bold;'>Date & Time:</td><td>" + formattedTime + "</td></tr>"
                + "</table>"
                + "<p style=\"margin-top:20px;font-size:15px;color:#444;\">You can now use your balance to make transfers or payments.</p>"
                + "<br/><p style=\"font-size:15px;color:#555;\">Thank you for using <strong>iPay</strong>!<br/>"
                + "Best regards,<br/><strong>The iPay Team</strong></p>"
                + "</div></body></html>";

        sendEmail(toEmail, subject, htmlBody);
    }

    public String getAdminMail() {
        return adminMail;
    }
}
