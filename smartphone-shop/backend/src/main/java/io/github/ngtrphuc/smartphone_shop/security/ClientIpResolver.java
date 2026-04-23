package io.github.ngtrphuc.smartphone_shop.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

    private final List<IpAddressMatcher> trustedProxyMatchers;

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String trustedProxies) {
        this.trustedProxyMatchers = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(IpAddressMatcher::new)
                .toList();
    }

    public String resolveClientKey(HttpServletRequest request) {
        String remoteAddr = normalizeAddress(request.getRemoteAddr());
        if (isTrustedProxy(remoteAddr)) {
            String forwardedClient = extractForwardedClient(request.getHeader("X-Forwarded-For"));
            if (forwardedClient != null) {
                return forwardedClient;
            }
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if ("unknown".equals(remoteAddr) || trustedProxyMatchers.isEmpty()) {
            return false;
        }
        return trustedProxyMatchers.stream().anyMatch(matcher -> matcher.matches(remoteAddr));
    }

    private String extractForwardedClient(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        for (String candidate : forwardedFor.split(",")) {
            String normalized = normalizeAddress(candidate);
            if (!"unknown".equals(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeAddress(String address) {
        if (address == null) {
            return "unknown";
        }
        String normalized = address.trim();
        return normalized.isBlank() ? "unknown" : normalized;
    }
}
