package io.github.ngtrphuc.smartphone_shop.service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;

@Service
public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_FULL_NAME_LENGTH = 100;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean register(String email, String fullName, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedFullName = normalizeFullName(fullName);
        String normalizedPassword = normalizePassword(rawPassword);

        if (normalizedEmail.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("Email is too long.");
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (normalizedFullName.length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }
        if (normalizedFullName.length() > MAX_FULL_NAME_LENGTH) {
            throw new IllegalArgumentException("Full name is too long.");
        }
        if (normalizedPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (normalizedPassword.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password is too long.");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return false;
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(normalizedFullName);
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        user.setRole("ROLE_USER");
        try {
            userRepository.save(user);
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFullName(String fullName) {
        return fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
    }

    private String normalizePassword(String rawPassword) {
        return rawPassword == null ? "" : rawPassword;
    }
}
