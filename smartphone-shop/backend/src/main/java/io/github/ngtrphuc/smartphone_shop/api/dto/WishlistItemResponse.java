package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long productId,
        String name,
        Double price,
        String imageUrl,
        Integer stock,
        LocalDateTime addedAt) {
}
