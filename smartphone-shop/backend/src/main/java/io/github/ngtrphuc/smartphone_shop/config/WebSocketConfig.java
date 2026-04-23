package io.github.ngtrphuc.smartphone_shop.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import io.github.ngtrphuc.smartphone_shop.security.JwtStompChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public WebSocketConfig(JwtStompChannelInterceptor jwtStompChannelInterceptor) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(resolveAllowedOrigins());
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }

    private @NonNull String[] resolveAllowedOrigins() {
        String[] resolved = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
        if (resolved.length == 0) {
            throw new IllegalStateException("app.cors.allowed-origins must contain at least one origin.");
        }
        boolean hasWildcard = Arrays.stream(resolved).anyMatch(this::isWildcardOriginPattern);
        if (hasWildcard) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins must not contain wildcard origins when credentials are enabled.");
        }
        return resolved;
    }

    private boolean isWildcardOriginPattern(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return false;
        }
        return normalized.contains("*");
    }
}
