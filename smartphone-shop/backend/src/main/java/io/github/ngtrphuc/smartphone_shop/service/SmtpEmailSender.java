package io.github.ngtrphuc.smartphone_shop.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String verificationLinkBaseUrl;

    public SmtpEmailSender(
            JavaMailSender mailSender,
            @Value("${app.email.from:noreply@smartphoneshop.local}") String fromAddress,
            @Value("${app.auth.verification-link-base-url:http://localhost:3000/verify-email}") String verificationLinkBaseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.verificationLinkBaseUrl = verificationLinkBaseUrl;
    }

    @Override
    @Async("emailExecutor")
    public void sendVerificationEmail(String toEmail, String token) {
        String safeToEmail = normalizeEmail(toEmail);
        String safeToken = Objects.requireNonNullElse(token, "");
        String normalizedBase = verificationLinkBaseUrl.endsWith("/")
                ? verificationLinkBaseUrl.substring(0, verificationLinkBaseUrl.length() - 1)
                : verificationLinkBaseUrl;
        String link = normalizedBase + "?token=" + safeToken;
        sendHtml(
                safeToEmail,
                "Verify your email",
                "<p>Thanks for signing up.</p><p>Please verify your email:</p><p><a href=\"" + link + "\">Verify now</a></p>");
    }

    @Override
    @Async("emailExecutor")
    public void sendWelcomeEmail(String toEmail, String fullName) {
        String safeToEmail = normalizeEmail(toEmail);
        String name = fullName == null || fullName.isBlank() ? "Customer" : fullName.trim();
        sendHtml(
                safeToEmail,
                "Welcome to Smartphone Shop",
                "<p>Hello " + name + ",</p><p>Your account is ready. Thank you for joining Smartphone Shop.</p>");
    }

    @Override
    @Async("emailExecutor")
    public void sendOrderConfirmation(String toEmail, String orderCode, Double totalAmount) {
        String safeToEmail = normalizeEmail(toEmail);
        String safeOrderCode = Objects.requireNonNullElse(orderCode, "N/A");
        double safeTotalAmount = totalAmount != null ? totalAmount : 0.0;
        sendHtml(
                safeToEmail,
                "Order confirmed: " + safeOrderCode,
                "<p>Your order <strong>" + safeOrderCode + "</strong> has been received.</p>"
                        + "<p>Total amount: <strong>" + safeTotalAmount + "</strong></p>");
    }

    @Override
    @Async("emailExecutor")
    public void sendOrderStatusUpdate(
            String toEmail,
            String orderCode,
            String oldStatus,
            String newStatus,
            String trackingNumber,
            String trackingCarrier) {
        String safeToEmail = normalizeEmail(toEmail);
        String safeOrderCode = Objects.requireNonNullElse(orderCode, "N/A");
        String safeOldStatus = Objects.requireNonNullElse(oldStatus, "unknown");
        String safeNewStatus = Objects.requireNonNullElse(newStatus, "unknown");
        String trackingLine = trackingNumber == null || trackingNumber.isBlank()
                ? ""
                : "<p>Tracking: <strong>" + trackingNumber + "</strong>"
                        + (trackingCarrier == null || trackingCarrier.isBlank()
                                ? ""
                                : " (" + trackingCarrier + ")")
                        + "</p>";
        sendHtml(
                safeToEmail,
                "Order update: " + safeOrderCode,
                "<p>Order <strong>" + safeOrderCode + "</strong> status changed from <strong>" + safeOldStatus
                        + "</strong> to <strong>" + safeNewStatus + "</strong>.</p>" + trackingLine);
    }

    private void sendHtml(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            String safeFromAddress = fromAddress;
            if (safeFromAddress == null || safeFromAddress.isBlank()) {
                safeFromAddress = "noreply@smartphoneshop.local";
            }
            String safeToEmail = normalizeEmail(toEmail);
            String safeSubject = subject == null ? "Smartphone Shop notification" : subject;
            String safeHtmlBody = htmlBody == null ? "" : htmlBody;
            String nonNullFromAddress = Objects.requireNonNull(safeFromAddress);

            helper.setFrom(nonNullFromAddress);
            helper.setTo(Objects.requireNonNull(safeToEmail));
            helper.setSubject(safeSubject);
            helper.setText(safeHtmlBody, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            log.warn("Failed to build email to={} subject={} cause={}", toEmail, subject, ex.getMessage());
        } catch (RuntimeException ex) {
            log.warn("Failed to send email to={} subject={} cause={}", toEmail, subject, ex.getMessage());
        }
    }

    private @NonNull String normalizeEmail(@Nullable String email) {
        String safeEmail = email == null ? "" : email.trim();
        if (safeEmail.isEmpty()) {
            throw new IllegalArgumentException("Email recipient must not be empty.");
        }
        return safeEmail;
    }
}
