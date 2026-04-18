package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.ChatService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CartItemRepository cartItemRepository;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ChatService chatService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void dashboard_shouldReturnForbidden_forNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void dashboard_shouldReturnOk_forAdmin() throws Exception {
        when(productRepository.count()).thenReturn(42L);
        when(orderService.getTotalItemsSold()).thenReturn(150L);
        when(orderService.countOrders()).thenReturn(21L);
        when(orderService.getTotalRevenue()).thenReturn(9876543.0);
        when(orderService.getRecentOrders(0, 10)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(42))
                .andExpect(jsonPath("$.totalItemsSold").value(150))
                .andExpect(jsonPath("$.totalOrders").value(21))
                .andExpect(jsonPath("$.recentOrders").isArray());
    }
}
