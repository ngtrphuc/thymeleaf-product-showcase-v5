package io.github.ngtrphuc.smartphone_shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, emailVerificationService);
    }

    @Test
    void register_shouldNormalizeEmailBeforeSaving() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean registered = authService.register("  User@Example.com  ", "  Nguyen   Phuc  ", "secret123");

        assertTrue(registered);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = Objects.requireNonNull(userCaptor.getValue());
        verify(emailVerificationService).sendVerification(savedUser);

        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals("Nguyen Phuc", savedUser.getFullName());
        assertEquals("encoded-secret", savedUser.getPassword());
    }

    @Test
    void register_shouldRejectInvalidEmail() {
        assertThrows(ValidationException.class,
                () -> authService.register("not-an-email", "Tester", "secret123"));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void register_shouldRejectPasswordsLongerThanBcryptLimit() {
        String tooLongPassword = "a".repeat(73);

        assertThrows(ValidationException.class,
                () -> authService.register("user@example.com", "Tester", tooLongPassword));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void register_shouldReturnFalseWhenUniqueConstraintWinsRace() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        boolean registered = authService.register("user@example.com", "Tester", "secret123");

        assertEquals(false, registered);
    }
}
