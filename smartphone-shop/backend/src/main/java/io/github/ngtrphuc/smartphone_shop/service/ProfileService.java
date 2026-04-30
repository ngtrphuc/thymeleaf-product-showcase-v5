package io.github.ngtrphuc.smartphone_shop.service;

import io.github.ngtrphuc.smartphone_shop.common.support.ValidationConstants;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private static final Pattern PHONE_PATTERN = ValidationConstants.PHONE_PATTERN;

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Transactional
    public User updateProfile(String email, String fullName, String phoneNumber, String defaultAddress) {
        String normalizedFullName = normalizeRequiredField(
                fullName, "Full name cannot be empty.", "Full name is too long.", 100);
        String normalizedPhoneNumber = normalizeOptionalField(
                phoneNumber, "Phone number is too long.", 30);
        String normalizedAddress = normalizeOptionalField(
                defaultAddress, "Address is too long.", 200);

        if (normalizedPhoneNumber != null && !PHONE_PATTERN.matcher(normalizedPhoneNumber).matches()) {
            throw new IllegalArgumentException("Phone number format is invalid.");
        }

        User user = findUserByEmail(email);
        user.setFullName(normalizedFullName);
        user.setPhoneNumber(normalizedPhoneNumber);
        user.setDefaultAddress(normalizedAddress);
        return userRepository.save(user);
    }

    private String normalizeRequiredField(String value, String emptyMessage, String tooLongMessage, int maxLength) {
        String normalized = normalizeOptionalField(value, tooLongMessage, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(emptyMessage);
        }
        return normalized;
    }

    private String normalizeOptionalField(String value, String tooLongMessage, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
        return normalized;
    }
}
