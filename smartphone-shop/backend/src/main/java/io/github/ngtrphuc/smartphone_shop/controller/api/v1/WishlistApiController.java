package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;
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
import io.github.ngtrphuc.smartphone_shop.model.WishlistItem;
import io.github.ngtrphuc.smartphone_shop.service.WishlistService;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistApiController {

    private final WishlistService wishlistService;
    private final ApiMapper apiMapper;

    public WishlistApiController(WishlistService wishlistService, ApiMapper apiMapper) {
        this.wishlistService = wishlistService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public WishlistResponse wishlist(Authentication authentication) {
        return currentWishlist(authentication);
    }

    @PostMapping("/items")
    public WishlistResponse add(@RequestBody WishlistAddRequest request, Authentication authentication) {
        Long productId = request.productId();
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        WishlistService.AddResult result = wishlistService.addItem(authentication.getName(), productId);
        if (result == WishlistService.AddResult.UNAVAILABLE) {
            throw new NoSuchElementException("Product not found.");
        }
        if (result == WishlistService.AddResult.ALREADY_EXISTS) {
            throw new IllegalStateException("This product is already in your wishlist.");
        }
        return currentWishlist(authentication);
    }

    @DeleteMapping("/items/{id}")
    public WishlistResponse remove(@PathVariable long id, Authentication authentication) {
        boolean removed = wishlistService.removeItem(authentication.getName(), id);
        if (!removed) {
            throw new NoSuchElementException("This product is not in your wishlist.");
        }
        return currentWishlist(authentication);
    }

    @GetMapping("/count")
    public long count(Authentication authentication) {
        return wishlistService.countWishlist(authentication.getName());
    }

    private WishlistResponse currentWishlist(Authentication authentication) {
        List<WishlistItem> items = wishlistService.getWishlist(authentication.getName());
        return new WishlistResponse(
                items.stream().map(apiMapper::toWishlistItemResponse).toList(),
                items.size());
    }

    private record WishlistAddRequest(Long productId) {
    }
}

