package io.github.ngtrphuc.smartphone_shop.config;

import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String configuredAdminEmail;

    @Value("${app.admin.password:}")
    private String configuredAdminPassword;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = normalize(configuredAdminEmail);
        String adminPassword = configuredAdminPassword == null ? "" : configuredAdminPassword.trim();

        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Skipping admin bootstrap because app.admin.email or app.admin.password is missing.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(adminEmail).matches()) {
            log.warn("Skipping admin bootstrap because '{}' is not a valid email.", adminEmail);
            return;
        }
        if (!isStrongAdminPassword(adminPassword)) {
            log.warn("Skipping admin bootstrap because app.admin.password does not meet complexity requirements.");
            return;
        }

        User adminUser = userRepository.findByEmailIgnoreCase(adminEmail).orElseGet(User::new);
        boolean isNewUser = adminUser.getId() == null;
        boolean passwordChanged = isNewUser
                || adminUser.getPassword() == null
                || !passwordEncoder.matches(adminPassword, adminUser.getPassword());
        boolean roleChanged = !"ROLE_ADMIN".equals(adminUser.getRole());

        adminUser.setEmail(adminEmail);
        adminUser.setFullName(adminUser.getFullName() == null || adminUser.getFullName().isBlank()
                ? "Administrator"
                : adminUser.getFullName());
        adminUser.setRole("ROLE_ADMIN");
        if (passwordChanged) {
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
        }

        if (isNewUser || passwordChanged || roleChanged) {
            userRepository.save(adminUser);
            log.info("Admin account {} for {}", isNewUser ? "initialized" : "synchronized", adminEmail);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isStrongAdminPassword(String password) {
        if (password == null || password.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < password.length(); i++) {
            char value = password.charAt(i);
            if (Character.isLowerCase(value)) {
                hasLower = true;
            } else if (Character.isUpperCase(value)) {
                hasUpper = true;
            } else if (Character.isDigit(value)) {
                hasDigit = true;
            } else {
                hasSymbol = true;
            }
        }
        return hasLower && hasUpper && hasDigit && hasSymbol;
    }
}
