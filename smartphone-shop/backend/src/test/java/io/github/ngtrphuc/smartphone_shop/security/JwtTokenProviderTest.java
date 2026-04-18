package io.github.ngtrphuc.smartphone_shop.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtTokenProviderTest {

    @Test
    void constructor_shouldFailFastForDefaultSecretWhenProdProfileIsActive() {
        JwtProperties properties = new JwtProperties();
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setActiveProfiles("prod");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtTokenProvider(properties, environment));

        assertEquals("JWT secret must be overridden in production.", exception.getMessage());
    }

    @Test
    void constructor_shouldAllowDefaultSecretOutsideProdProfile() {
        JwtProperties properties = new JwtProperties();
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        environment.setActiveProfiles("dev");

        assertDoesNotThrow(() -> new JwtTokenProvider(properties, environment));
    }
}
