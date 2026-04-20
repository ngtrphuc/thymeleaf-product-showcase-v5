package io.github.ngtrphuc.smartphone_shop.controller;

import java.net.URI;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    private final String frontendUrl;

    @Autowired
    public RootController(@Value("${app.frontend.url:http://localhost:3000}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/")
    public Object root(@RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader) {
        if (acceptsHtml(acceptHeader)) {
            URI targetUri = Objects.requireNonNull(URI.create(frontendUrl), "Frontend URL must resolve to a URI.");
            return ResponseEntity.status(302)
                    .location(targetUri)
                    .build();
        }
        return new RootResponse(
                "smartphone-shop-api",
                "UP",
                frontendUrl,
                "/swagger-ui/index.html");
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
