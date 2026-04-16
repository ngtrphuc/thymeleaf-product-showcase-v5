package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.NoSuchElementException;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/v1/cart")
public class CartApiController {

    private final CartService cartService;
    private final ApiMapper apiMapper;

    public CartApiController(CartService cartService, ApiMapper apiMapper) {
        this.cartService = cartService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public CartResponse cart(Authentication authentication, HttpSession session) {
        return currentCart(authentication, session);
    }

    @PostMapping("/items")
    public CartResponse addItem(@RequestBody AddItemRequest request,
            Authentication authentication,
            HttpSession session) {
        Long productId = request.productId();
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        CartService.AddItemResult result = cartService.addItem(resolveEmail(authentication), session,
                productId, request.quantity() != null ? request.quantity() : 1);
        cartService.syncCartCount(session, resolveEmail(authentication));
        if (result == CartService.AddItemResult.UNAVAILABLE) {
            throw new NoSuchElementException("Product not found or unavailable.");
        }
        if (result == CartService.AddItemResult.LIMIT_REACHED) {
            throw new IllegalStateException("You've already added the maximum available stock for this product.");
        }
        return currentCart(authentication, session);
    }

    @PostMapping("/items/{id}/increase")
    public CartResponse increase(@PathVariable long id, Authentication authentication, HttpSession session) {
        cartService.increaseItem(resolveEmail(authentication), session, id);
        cartService.syncCartCount(session, resolveEmail(authentication));
        return currentCart(authentication, session);
    }

    @PostMapping("/items/{id}/decrease")
    public CartResponse decrease(@PathVariable long id, Authentication authentication, HttpSession session) {
        cartService.decreaseItem(resolveEmail(authentication), session, id);
        cartService.syncCartCount(session, resolveEmail(authentication));
        return currentCart(authentication, session);
    }

    @DeleteMapping("/items/{id}")
    public CartResponse remove(@PathVariable long id, Authentication authentication, HttpSession session) {
        cartService.removeItem(resolveEmail(authentication), session, id);
        cartService.syncCartCount(session, resolveEmail(authentication));
        return currentCart(authentication, session);
    }

    @DeleteMapping
    public CartResponse clear(Authentication authentication, HttpSession session) {
        cartService.clearCart(resolveEmail(authentication), session);
        return currentCart(authentication, session);
    }

    private CartResponse currentCart(Authentication authentication, HttpSession session) {
        String email = resolveEmail(authentication);
        return apiMapper.toCartResponse(
                cartService.getCart(email, session),
                email != null);
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    private record AddItemRequest(Long productId, Integer quantity) {
    }
}

