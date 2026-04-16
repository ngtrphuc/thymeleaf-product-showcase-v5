package io.github.ngtrphuc.smartphone_shop.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.CompareService;

@ExtendWith(MockitoExtension.class)
class CompareControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CompareService compareService;

    @Test
    void add_shouldUseCompareServiceAndRedirectBack() {
        CompareController controller = new CompareController(productRepository, compareService);
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        when(compareService.addItem(null, session, 1L))
                .thenReturn(CompareService.AddResult.ADDED);

        String redirect = controller.add(1L, "/compare", null, session, ra);

        assertEquals("redirect:/compare", redirect);
        assertEquals("Added to compare list.", ra.getFlashAttributes().get("toast"));
        verify(compareService).addItem(null, session, 1L);
    }

    @Test
    void comparePage_shouldRenderProductsInSessionOrder() {
        CompareController controller = new CompareController(productRepository, compareService);
        MockHttpSession session = new MockHttpSession();

        Product first = new Product();
        first.setId(1L);
        first.setName("Model 1");

        Product third = new Product();
        third.setId(3L);
        third.setName("Model 3");

        Product candidate = new Product();
        candidate.setId(2L);
        candidate.setName("Model 2");

        when(compareService.getCompareIds(null, session)).thenReturn(List.of(3L, 1L));
        when(productRepository.findAllByIdIn(List.of(3L, 1L))).thenReturn(List.of(first, third));
        when(productRepository.findByIdNotInOrderByNameAsc(List.of(3L, 1L))).thenReturn(List.of(candidate));

        Model model = new ExtendedModelMap();
        String view = controller.comparePage(null, session, model);

        List<?> products = assertInstanceOf(List.class, model.getAttribute("products"));
        Product firstOrdered = assertInstanceOf(Product.class, products.get(0));
        Product secondOrdered = assertInstanceOf(Product.class, products.get(1));

        assertEquals("compare", view);
        assertEquals(List.of(3L, 1L), List.of(firstOrdered.getId(), secondOrdered.getId()));
    }

    @Test
    void remove_shouldDelegateToServiceAndRedirect() {
        CompareController controller = new CompareController(productRepository, compareService);
        MockHttpSession session = new MockHttpSession();

        String redirect = controller.remove(9L, "/compare", null, session);

        assertEquals("redirect:/compare", redirect);
        verify(compareService).removeItem(null, session, 9L);
    }

    @Test
    void comparePage_shouldUseAuthenticatedEmail() {
        CompareController controller = new CompareController(productRepository, compareService);
        MockHttpSession session = new MockHttpSession();
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn("user@example.com");
        when(compareService.getCompareIds("user@example.com", session)).thenReturn(List.of());
        when(productRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        String view = controller.comparePage(auth, session, new ExtendedModelMap());

        assertEquals("compare", view);
        verify(compareService).getCompareIds("user@example.com", session);
    }
}
