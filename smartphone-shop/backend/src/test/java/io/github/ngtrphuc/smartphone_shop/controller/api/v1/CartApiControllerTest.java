package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.service.CartService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void cartFlow_shouldAddItemAndReturnItInCart() throws Exception {
        CartItem cartItem = new CartItem(1L, "Phone A", 100.0, 2);
        when(cartService.addItem("user@example.com", null, 1L, 2))
                .thenReturn(CartService.AddItemResult.ADDED);
        when(cartService.getUserCart("user@example.com"))
                .thenReturn(List.of(cartItem));

        mockMvc.perform(post("/api/v1/cart/items")
                .contentType("application/json")
                .content("""
                        {
                          "productId": 1,
                          "quantity": 2
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1));

        verify(cartService).addItem(eq("user@example.com"), eq(null), eq(1L), eq(2));
        verify(cartService, times(2)).getUserCart("user@example.com");
    }

    @Test
    void cart_shouldReturnUnauthorizedForAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addItem_shouldDefaultQuantityToOne_whenQuantityMissing() throws Exception {
        CartItem cartItem = new CartItem(2L, "Phone B", 250.0, 1);
        when(cartService.addItem("user@example.com", null, 2L, 1))
                .thenReturn(CartService.AddItemResult.ADDED);
        when(cartService.getUserCart("user@example.com")).thenReturn(List.of(cartItem));

        mockMvc.perform(post("/api/v1/cart/items")
                .contentType("application/json")
                .content("""
                        {
                          "productId": 2
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(2))
                .andExpect(jsonPath("$.items[0].quantity").value(1));

        verify(cartService).addItem(eq("user@example.com"), eq(null), eq(2L), eq(1));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addItem_shouldReturnBadRequest_whenProductIdMissing() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                .contentType("application/json")
                .content("""
                        {
                          "quantity": 1
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addItem_shouldReturnNotFound_whenProductUnavailable() throws Exception {
        when(cartService.addItem("user@example.com", null, 404L, 1))
                .thenReturn(CartService.AddItemResult.UNAVAILABLE);

        mockMvc.perform(post("/api/v1/cart/items")
                .contentType("application/json")
                .content("""
                        {
                          "productId": 404
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addItem_shouldReturnConflict_whenLimitReached() throws Exception {
        when(cartService.addItem("user@example.com", null, 1L, 1))
                .thenReturn(CartService.AddItemResult.LIMIT_REACHED);

        mockMvc.perform(post("/api/v1/cart/items")
                .contentType("application/json")
                .content("""
                        {
                          "productId": 1
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void addItem_shouldReturnInvalidBody_whenJsonMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                .contentType("application/json")
                .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BODY"));
    }

    @Test
    void addItem_shouldReturnUnauthorizedForAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                .contentType("application/json")
                .content("""
                        {
                          "productId": 1,
                          "quantity": 1
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
