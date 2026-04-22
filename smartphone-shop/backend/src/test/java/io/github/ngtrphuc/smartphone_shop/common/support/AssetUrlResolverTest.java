package io.github.ngtrphuc.smartphone_shop.common.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AssetUrlResolverTest {

    @Test
    void resolve_shouldReturnNullForBlankInput() {
        AssetUrlResolver resolver = new AssetUrlResolver("https://cdn.smartphone-shop.com");
        assertNull(resolver.resolve(" "));
    }

    @Test
    void resolve_shouldPrefixRelativePathWhenBaseUrlConfigured() {
        AssetUrlResolver resolver = new AssetUrlResolver("https://cdn.smartphone-shop.com/");
        assertEquals("https://cdn.smartphone-shop.com/images/iphone17.png", resolver.resolve("/images/iphone17.png"));
        assertEquals("https://cdn.smartphone-shop.com/images/iphone17.png", resolver.resolve("images/iphone17.png"));
    }

    @Test
    void resolve_shouldKeepAbsoluteUrlUnchanged() {
        AssetUrlResolver resolver = new AssetUrlResolver("https://cdn.smartphone-shop.com");
        assertEquals("https://example.com/a.png", resolver.resolve("https://example.com/a.png"));
    }

    @Test
    void resolve_shouldKeepOriginalPathWhenNoBaseUrl() {
        AssetUrlResolver resolver = new AssetUrlResolver("");
        assertEquals("/images/iphone17.png", resolver.resolve("/images/iphone17.png"));
    }
}
