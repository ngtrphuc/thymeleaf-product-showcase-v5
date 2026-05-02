package io.github.ngtrphuc.smartphone_shop.service;

public interface EmailSender {
    void sendVerificationEmail(String toEmail, String token);

    void sendWelcomeEmail(String toEmail, String fullName);

    void sendOrderConfirmation(String toEmail, String orderCode, Double totalAmount);

    void sendOrderStatusUpdate(
            String toEmail,
            String orderCode,
            String oldStatus,
            String newStatus,
            String trackingNumber,
            String trackingCarrier);
}
