package io.github.ngtrphuc.smartphone_shop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RootControllerTest {

    @Test
    void root_shouldRedirectToFrontend_forBrowserRequest() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RootController("http://localhost:3000"))
                .build();

        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:3000"));
    }

    @Test
    void root_shouldReturnApiEntryInformation_forJsonRequest() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RootController("http://localhost:3000"))
                .build();

        mockMvc.perform(get("/").accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("smartphone-shop-api"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.frontendUrl").value("http://localhost:3000"))
                .andExpect(jsonPath("$.docsUrl").value("/swagger-ui/index.html"));
    }
}
