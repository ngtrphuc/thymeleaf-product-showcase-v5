package io.github.ngtrphuc.smartphone_shop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void root_shouldRedirectToFrontend_forBrowserRequest() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:3000"));
    }

    @Test
    void root_shouldReturnApiEntryInformation_forJsonRequest() throws Exception {
        mockMvc.perform(get("/").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("smartphone-shop-api"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.frontendUrl").value("http://localhost:3000"))
                .andExpect(jsonPath("$.docsUrl").value("/swagger-ui/index.html"));
    }
}
