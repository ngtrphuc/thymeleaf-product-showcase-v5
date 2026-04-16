package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.util.List;

public record WishlistResponse(
        List<WishlistItemResponse> items,
        long count) {
}
