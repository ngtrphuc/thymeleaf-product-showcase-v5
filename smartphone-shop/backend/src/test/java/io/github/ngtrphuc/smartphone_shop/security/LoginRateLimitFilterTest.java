package io.github.ngtrphuc.smartphone_shop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.login-rate-limit.max-attempts=2",
        "app.security.login-rate-limit.window-seconds=300"
})
class LoginRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void login_shouldReturnTooManyRequestsAfterConfiguredLimit() throws Exception {
        String requestBody = """
                {"email":"unknown@example.com","password":"wrong-password"}
                """;
        String clientIp = "198.51.100.99";
        MediaType jsonMediaType = Objects.requireNonNull(MediaType.APPLICATION_JSON);

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", clientIp)
                .contentType(jsonMediaType)
                .content(requestBody))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", clientIp)
                .contentType(jsonMediaType)
                .content(requestBody))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", clientIp)
                .contentType(jsonMediaType)
                .content(requestBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }
}
