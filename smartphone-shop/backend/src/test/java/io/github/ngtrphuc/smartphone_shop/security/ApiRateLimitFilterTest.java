package io.github.ngtrphuc.smartphone_shop.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.api-rate-limit.enabled=true",
        "app.security.api-rate-limit.max-requests=2",
        "app.security.api-rate-limit.window-seconds=300"
})
class ApiRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicApi_shouldReturnTooManyRequestsAfterConfiguredLimit() throws Exception {
        String clientIp = "203.0.113.81";

        mockMvc.perform(get("/api/v1/products")
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products")
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products")
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }
}
