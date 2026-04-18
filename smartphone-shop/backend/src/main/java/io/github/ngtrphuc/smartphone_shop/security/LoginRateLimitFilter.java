package io.github.ngtrphuc.smartphone_shop.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String TOO_MANY_REQUESTS_CODE = "TOO_MANY_REQUESTS";
    private static final String TOO_MANY_REQUESTS_MESSAGE = "Too many login attempts. Please try again later.";

    private final Map<String, AttemptWindow> attemptsByClient = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long windowMillis;

    public LoginRateLimitFilter(
            @Value("${app.security.login-rate-limit.max-attempts:8}") int maxAttempts,
            @Value("${app.security.login-rate-limit.window-seconds:60}") long windowSeconds) {
        if (maxAttempts < 1) {
            throw new IllegalStateException("app.security.login-rate-limit.max-attempts must be greater than 0.");
        }
        if (windowSeconds < 1) {
            throw new IllegalStateException("app.security.login-rate-limit.window-seconds must be greater than 0.");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowSeconds * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        return !("POST".equalsIgnoreCase(request.getMethod()) && LOGIN_PATH.equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        String clientKey = resolveClientKey(request);

        AttemptWindow updated = attemptsByClient.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStartMillis >= windowMillis) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(existing.windowStartMillis, existing.attempts + 1);
        });

        if (updated != null && updated.attempts > maxAttempts) {
            long elapsed = Math.max(0L, now - updated.windowStartMillis);
            long retryAfterSeconds = Math.max(1L, (windowMillis - elapsed + 999L) / 1000L);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"code\":\"" + TOO_MANY_REQUESTS_CODE + "\",\"message\":\""
                    + TOO_MANY_REQUESTS_MESSAGE + "\"}");
            return;
        }

        cleanupExpiredEntries(now);
        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                return first;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "unknown" : remoteAddr;
    }

    private void cleanupExpiredEntries(long now) {
        if (attemptsByClient.size() < 5000) {
            return;
        }
        attemptsByClient.entrySet().removeIf(entry -> now - entry.getValue().windowStartMillis >= windowMillis);
    }

    private record AttemptWindow(long windowStartMillis, int attempts) {
    }
}
