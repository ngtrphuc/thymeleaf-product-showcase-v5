package io.github.ngtrphuc.smartphone_shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevFrontendBootstrapTest {

    @TempDir
    Path tempDir;

    @Test
    void isLocalFrontendUrl_shouldRecognizeOnlyLoopbackHosts() {
        assertTrue(DevFrontendBootstrap.isLocalFrontendUrl("http://localhost:3000"));
        assertTrue(DevFrontendBootstrap.isLocalFrontendUrl("http://127.0.0.1:3000"));
        assertFalse(DevFrontendBootstrap.isLocalFrontendUrl("https://example.com"));
        assertFalse(DevFrontendBootstrap.isLocalFrontendUrl("not-a-url"));
    }

    @Test
    void buildFrontendEnvLocalContent_shouldPointFrontendAtBackend() {
        String content = DevFrontendBootstrap.buildFrontendEnvLocalContent("http://localhost:8080");

        assertTrue(content.contains("API_BASE_URL=http://localhost:8080"));
        assertTrue(content.contains("NEXT_PUBLIC_API_BASE_URL=http://localhost:8080"));
    }

    @Test
    void ensureFrontendEnvLocal_shouldCreateMissingFile() throws IOException {
        DevFrontendBootstrap.ensureFrontendEnvLocal(tempDir, "http://localhost:8080");

        String content = Files.readString(tempDir.resolve(".env.local"), StandardCharsets.UTF_8);

        assertTrue(content.contains("API_BASE_URL=http://localhost:8080"));
        assertTrue(content.contains("NEXT_PUBLIC_API_BASE_URL=http://localhost:8080"));
    }

    @Test
    void ensureFrontendEnvLocal_shouldSynchronizeExistingKeysToResolvedBackend() throws IOException {
        Path envLocal = tempDir.resolve(".env.local");
        Files.writeString(envLocal, "API_BASE_URL=http://localhost:9999" + System.lineSeparator(), StandardCharsets.UTF_8);

        DevFrontendBootstrap.ensureFrontendEnvLocal(tempDir, "http://localhost:8080");

        String content = Files.readString(envLocal, StandardCharsets.UTF_8);
        assertTrue(content.contains("API_BASE_URL=http://localhost:8080"));
        assertTrue(content.contains("NEXT_PUBLIC_API_BASE_URL=http://localhost:8080"));
    }

    @Test
    void ensureFrontendEnvLocal_shouldReplaceBothKeysWhenAlreadyPresent() throws IOException {
        Path envLocal = tempDir.resolve(".env.local");
        Files.writeString(
                envLocal,
                "API_BASE_URL=http://localhost:9090" + System.lineSeparator()
                        + "NEXT_PUBLIC_API_BASE_URL=http://localhost:9090" + System.lineSeparator(),
                StandardCharsets.UTF_8);

        DevFrontendBootstrap.ensureFrontendEnvLocal(tempDir, "http://localhost:8080");

        String content = Files.readString(envLocal, StandardCharsets.UTF_8);
        assertTrue(content.contains("API_BASE_URL=http://localhost:8080"));
        assertTrue(content.contains("NEXT_PUBLIC_API_BASE_URL=http://localhost:8080"));
    }

    @Test
    void resolveBackendBaseUrl_shouldRespectCustomServerPortArgument() {
        String backendBaseUrl = DevFrontendBootstrap.resolveBackendBaseUrl(new String[] { "--server.port=9090" });

        assertEquals("http://localhost:9090", backendBaseUrl);
    }
}
