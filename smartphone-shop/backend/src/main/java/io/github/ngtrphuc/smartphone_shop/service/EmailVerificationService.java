package io.github.ngtrphuc.smartphone_shop.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.model.EmailVerificationToken;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.EmailVerificationTokenRepository;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;

@Service
public class EmailVerificationService {

    private static final int TOKEN_EXPIRY_HOURS = 24;
    private static final int MAX_RESEND_PER_DAY = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailSender emailSender) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void sendVerification(User user) {
        if (user == null || user.getId() == null) {
            throw new ValidationException("User is required for email verification.");
        }
        if (user.isEmailVerified()) {
            return;
        }

        long attemptsInLastDay = tokenRepository.countByUserIdAndCreatedAtAfter(
                user.getId(),
                LocalDateTime.now().minusDays(1));
        if (attemptsInLastDay >= MAX_RESEND_PER_DAY) {
            throw new ValidationException("Too many verification requests. Please try again later.");
        }

        tokenRepository.markAllUnusedAsUsed(user.getId());

        EmailVerificationToken tokenEntity = new EmailVerificationToken();
        tokenEntity.setUser(user);
        tokenEntity.setToken(generateSecureToken());
        tokenEntity.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        tokenRepository.save(tokenEntity);

        emailSender.sendVerificationEmail(user.getEmail(), tokenEntity.getToken());
    }

    @Transactional
    public void verify(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isBlank()) {
            throw new ValidationException("Verification token is required.");
        }

        EmailVerificationToken tokenEntity = tokenRepository.findByTokenAndUsedFalse(normalized)
                .orElseThrow(() -> new ValidationException("Invalid or expired verification token."));
        if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Verification token has expired.");
        }

        tokenEntity.setUsed(true);
        tokenRepository.save(tokenEntity);

        User user = tokenEntity.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ValidationException("User not found."));
        if (user.isEmailVerified()) {
            throw new ValidationException("Your email is already verified.");
        }
        sendVerification(user);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
