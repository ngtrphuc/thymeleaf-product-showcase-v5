package io.github.ngtrphuc.smartphone_shop.common.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AssetUrlResolver {

    private final String baseUrl;

    public AssetUrlResolver(@Value("${app.assets.base-url:}") String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String resolve(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String path = rawPath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        if (baseUrl.isBlank()) {
            return path;
        }
        if (path.startsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private String normalizeBaseUrl(String configured) {
        if (configured == null) {
            return "";
        }
        String trimmed = configured.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
