package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.CompareService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompareApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompareService compareService;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void anonymousCompareFlow_shouldUseSessionBackedCompare() throws Exception {
        Product phone = new Product();
        phone.setId(1L);
        phone.setName("Phone A");
        phone.setPrice(100.0);
        phone.setStock(5);

        when(compareService.addItem(eq(null), any(), eq(1L))).thenReturn(CompareService.AddResult.ADDED);
        when(compareService.getCompareIds(eq(null), any())).thenReturn(List.of(1L));
        when(compareService.getMaxCompare()).thenReturn(3);
        when(productRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(phone));

        mockMvc.perform(post("/api/v1/compare/items")
                .contentType("application/json")
                .content("""
                        {
                          "productId": 1
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]").value(1))
                .andExpect(jsonPath("$.products[0].id").value(1))
                .andExpect(jsonPath("$.maxCompare").value(3));

        mockMvc.perform(get("/api/v1/compare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]").value(1))
                .andExpect(jsonPath("$.products[0].id").value(1));

        verify(compareService).addItem(eq(null), any(), eq(1L));
        verify(compareService, times(2)).getCompareIds(eq(null), any());
    }

    @Test
    void anonymousClear_shouldBeAllowed() throws Exception {
        when(compareService.getCompareIds(eq(null), any())).thenReturn(List.of());
        when(compareService.getMaxCompare()).thenReturn(3);

        mockMvc.perform(delete("/api/v1/compare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids").isArray())
                .andExpect(jsonPath("$.maxCompare").value(3));

        verify(compareService).clear(eq(null), any());
    }

    @Test
    void anonymousReplace_shouldPersistRequestedOrder() throws Exception {
        Product first = new Product();
        first.setId(3L);
        first.setName("Phone C");
        first.setPrice(300.0);
        first.setStock(5);

        Product second = new Product();
        second.setId(1L);
        second.setName("Phone A");
        second.setPrice(100.0);
        second.setStock(7);

        when(compareService.getCompareIds(eq(null), any())).thenReturn(List.of(3L, 1L));
        when(compareService.getMaxCompare()).thenReturn(3);
        when(productRepository.findAllByIdIn(List.of(3L, 1L))).thenReturn(List.of(first, second));

        mockMvc.perform(put("/api/v1/compare")
                .contentType("application/json")
                .content("""
                        {
                          "productIds": [3, 1]
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]").value(3))
                .andExpect(jsonPath("$.ids[1]").value(1))
                .andExpect(jsonPath("$.products[0].id").value(3))
                .andExpect(jsonPath("$.products[1].id").value(1));

        verify(compareService).saveCompareIds(eq(null), any(), eq(List.of(3L, 1L)));
    }
}
