package io.github.ngtrphuc.smartphone_shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LogOnlyEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LogOnlyEmailSender.class);

    private final String verificationLinkBaseUrl;

    public LogOnlyEmailSender(
            @Value("${app.auth.verification-link-base-url:http://localhost:3000/verify-email}") String verificationLinkBaseUrl) {
        this.verificationLinkBaseUrl = verificationLinkBaseUrl;
    }

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        String normalizedBase = verificationLinkBaseUrl.endsWith("/")
                ? verificationLinkBaseUrl.substring(0, verificationLinkBaseUrl.length() - 1)
                : verificationLinkBaseUrl;
        String link = normalizedBase + "?token=" + token;
        log.info("=== VERIFY EMAIL ===\nTo: {}\nLink: {}", toEmail, link);
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        log.info("=== WELCOME EMAIL ===\nTo: {}\nName: {}", toEmail, fullName);
    }

    @Override
    public void sendOrderConfirmation(String toEmail, String orderCode, Double totalAmount) {
        log.info("=== ORDER CONFIRMATION EMAIL ===\nTo: {}\nOrder: {}\nTotal: {}", toEmail, orderCode, totalAmount);
    }

    @Override
    public void sendOrderStatusUpdate(
            String toEmail,
            String orderCode,
            String oldStatus,
            String newStatus,
            String trackingNumber,
            String trackingCarrier) {
        log.info(
                "=== ORDER STATUS EMAIL ===\nTo: {}\nOrder: {}\nFrom: {}\nTo: {}\nTracking: {} ({})",
                toEmail,
                orderCode,
                oldStatus,
                newStatus,
                trackingNumber,
                trackingCarrier);
    }
}
