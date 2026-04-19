package io.github.ngtrphuc.smartphone_shop.controller;

import java.net.URI;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class RootController {

    private static final Duration FRONTEND_CHECK_TIMEOUT = Duration.ofMillis(400);

    private final String frontendUrl;
    private final boolean redirectOnRoot;
    private final BooleanSupplier frontendAvailabilityChecker;

    @Autowired
    public RootController(
            @Value("${app.frontend.url:http://localhost:3000}") String frontendUrl,
            @Value("${app.frontend.redirect-on-root:false}") boolean redirectOnRoot) {
        this(frontendUrl, redirectOnRoot, null);
    }

    RootController(String frontendUrl, boolean redirectOnRoot, BooleanSupplier frontendAvailabilityChecker) {
        this.frontendUrl = frontendUrl;
        this.redirectOnRoot = redirectOnRoot;
        this.frontendAvailabilityChecker = frontendAvailabilityChecker != null
                ? frontendAvailabilityChecker
                : this::isFrontendAvailable;
    }

    @GetMapping("/")
    public Object root(@RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader) {
        if (acceptsHtml(acceptHeader)) {
            if (redirectOnRoot || frontendAvailabilityChecker.getAsBoolean()) {
                URI targetUri = Objects.requireNonNull(URI.create(frontendUrl), "Frontend URL must resolve to a URI.");
                return ResponseEntity.status(302)
                        .location(targetUri)
                        .build();
            }
            return ResponseEntity.ok()
                    .contentType(Objects.requireNonNull(MediaType.TEXT_HTML))
                    .body(browserLandingPage());
        }
        return new RootResponse(
                "smartphone-shop-api",
                "UP",
                frontendUrl,
                "/swagger-ui/index.html");
    }

    private String browserLandingPage() {
        String safeFrontendUrl = HtmlUtils.htmlEscape(frontendUrl);
        if (safeFrontendUrl == null) {
            safeFrontendUrl = frontendUrl;
        }
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Smartphone Shop API</title>
                  <style>
                    :root {
                      color-scheme: light dark;
                      font-family: "Segoe UI", Arial, sans-serif;
                    }
                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: grid;
                      place-items: center;
                      background: linear-gradient(135deg, #0f172a, #1e293b);
                      color: #e2e8f0;
                    }
                    main {
                      width: min(720px, calc(100%% - 32px));
                      padding: 32px;
                      border-radius: 24px;
                      background: rgba(15, 23, 42, 0.88);
                      box-shadow: 0 24px 64px rgba(15, 23, 42, 0.4);
                    }
                    h1 {
                      margin-top: 0;
                      margin-bottom: 12px;
                      font-size: clamp(2rem, 4vw, 2.8rem);
                    }
                    p {
                      line-height: 1.6;
                      color: #cbd5e1;
                    }
                    .actions {
                      display: flex;
                      flex-wrap: wrap;
                      gap: 12px;
                      margin: 24px 0;
                    }
                    a {
                      text-decoration: none;
                    }
                    .button {
                      display: inline-flex;
                      align-items: center;
                      justify-content: center;
                      padding: 12px 16px;
                      border-radius: 999px;
                      background: #38bdf8;
                      color: #082f49;
                      font-weight: 700;
                    }
                    .button.secondary {
                      background: rgba(148, 163, 184, 0.16);
                      color: #e2e8f0;
                    }
                    code {
                      display: block;
                      padding: 12px 14px;
                      border-radius: 12px;
                      background: rgba(15, 23, 42, 0.72);
                      color: #f8fafc;
                      overflow-x: auto;
                    }
                    .hint {
                      margin-top: 20px;
                      padding: 16px;
                      border-radius: 16px;
                      background: rgba(56, 189, 248, 0.12);
                    }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>Smartphone Shop backend is running</h1>
                    <p>
                      The API is available on port 8080. The storefront runs separately on port 3000,
                      so opening the backend directly no longer sends the browser to a frontend server that may be offline.
                    </p>
                    <div class="actions">
                      <a class="button" href="/swagger-ui/index.html">Open API docs</a>
                      <a class="button secondary" href="%s">Open storefront</a>
                      <a class="button secondary" href="/actuator/health">Health check</a>
                    </div>
                    <p>Start the Next.js storefront when you want the full customer UI:</p>
                    <code>cd frontend-next && npm.cmd run dev</code>
                    <div class="hint">
                      <strong>Storefront URL:</strong> %s
                    </div>
                  </main>
                </body>
                </html>
                """.formatted(safeFrontendUrl, safeFrontendUrl);
    }

    private boolean isFrontendAvailable() {
        try {
            URI frontendUri = URI.create(frontendUrl);
            String host = frontendUri.getHost();
            int port = frontendUri.getPort() > 0 ? frontendUri.getPort() : 80;
            if (host == null || host.isBlank()) {
                return false;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), (int) FRONTEND_CHECK_TIMEOUT.toMillis());
                return true;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean acceptsHtml(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return true;
        }
        try {
            return MediaType.parseMediaTypes(acceptHeader).stream()
                    .anyMatch(mediaType -> mediaType.isCompatibleWith(MediaType.TEXT_HTML));
        } catch (IllegalArgumentException ex) {
            return true;
        }
    }

    public record RootResponse(
            String service,
            String status,
            String frontendUrl,
            String docsUrl) {
    }
}
