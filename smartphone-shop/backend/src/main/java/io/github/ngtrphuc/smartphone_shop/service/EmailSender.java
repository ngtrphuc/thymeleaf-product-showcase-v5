package io.github.ngtrphuc.smartphone_shop.service;

public interface EmailSender {
    void sendVerificationEmail(String toEmail, String token);
}
