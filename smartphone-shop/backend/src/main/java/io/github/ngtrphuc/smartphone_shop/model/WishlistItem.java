package io.github.ngtrphuc.smartphone_shop.model;

import java.time.LocalDateTime;

public class WishlistItem {

    private Long productId;
    private String name;
    private Double price;
    private String imageUrl;
    private Integer stock;
    private LocalDateTime addedAt;

    public WishlistItem() {
    }

    public WishlistItem(Long productId, String name, Double price, String imageUrl,
            Integer stock, LocalDateTime addedAt) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.addedAt = addedAt;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
