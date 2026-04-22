package io.github.ngtrphuc.smartphone_shop.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_REQUESTS_CODE = "TOO_MANY_REQUESTS";
    private static final String TOO_MANY_REQUESTS_MESSAGE =
            "Too many API requests. Please retry after a short delay.";

    private final boolean enabled;
    private final Cache<String, AttemptWindow> attemptsByClient;
    private final int maxRequests;
    private final long windowMillis;
    private final List<String> excludedPaths;

    public ApiRateLimitFilter(
            @Value("${app.security.api-rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.api-rate-limit.max-requests:180}") int maxRequests,
            @Value("${app.security.api-rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${app.security.api-rate-limit.max-clients:200000}") long maxClients,
            @Value("${app.security.api-rate-limit.excluded-paths:/api/v1/auth/login,/api/v1/auth/register,/api/v1/auth/logout,/api/v1/auth/me}") String excludedPaths) {
        if (maxRequests < 1) {
            throw new IllegalStateException("app.security.api-rate-limit.max-requests must be greater than 0.");
        }
        if (windowSeconds < 1) {
            throw new IllegalStateException("app.security.api-rate-limit.window-seconds must be greater than 0.");
        }
        if (maxClients < 1) {
            throw new IllegalStateException("app.security.api-rate-limit.max-clients must be greater than 0.");
        }
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000L;
        this.excludedPaths = Arrays.stream(excludedPaths.split(","))
                .map(String::trim)
                .filter(path -> !path.isBlank())
                .toList();
        this.attemptsByClient = Caffeine.newBuilder()
                .expireAfterWrite(windowSeconds, TimeUnit.SECONDS)
                .maximumSize(maxClients)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = resolvePath(request);
        if (!path.startsWith("/api/v1/")) {
            return true;
        }
        return excludedPaths.stream().anyMatch(excluded ->
                path.equals(excluded) || path.startsWith(excluded + "/"));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        String clientKey = resolveClientKey(request);

        AttemptWindow updated = attemptsByClient.asMap().compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing.windowStartMillis >= windowMillis) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(existing.windowStartMillis, existing.attempts + 1);
        });

        if (updated != null && updated.attempts > maxRequests) {
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

    private String resolvePath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        return path == null ? "" : path;
    }

    private record AttemptWindow(long windowStartMillis, int attempts) {
    }
}
