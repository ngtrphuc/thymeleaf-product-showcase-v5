package io.github.ngtrphuc.smartphone_shop.model;

public class CartItem {
    private Long id;
    private Long variantId;
    private String variantSku;
    private String variantLabel;
    private String name;
    private Double price;
    private int quantity;
    private String imageUrl;
    private int availableStock;

    public CartItem() {
    }

    public CartItem(Long id, String name, Double price, int quantity) {
        this(id, null, null, null, name, price, quantity, null, 0);
    }

    public CartItem(Long id, String name, Double price, int quantity, String imageUrl, int availableStock) {
        this(id, null, null, null, name, price, quantity, imageUrl, availableStock);
    }

    public CartItem(Long id,
            Long variantId,
            String variantSku,
            String variantLabel,
            String name,
            Double price,
            int quantity,
            String imageUrl,
            int availableStock) {
        this.id = id;
        this.variantId = variantId;
        this.variantSku = variantSku;
        this.variantLabel = variantLabel;
        this.name = name;
        this.price = price != null ? price : 0.0;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.availableStock = availableStock;
    }

    public Long getId() { return id; }
    public Long getVariantId() { return variantId; }
    public String getVariantSku() { return variantSku; }
    public String getVariantLabel() { return variantLabel; }
    public String getName() { return name; }
    public Double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getImageUrl() { return imageUrl; }
    public int getAvailableStock() { return availableStock; }

    public void setId(Long id) { this.id = id; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public void setVariantSku(String variantSku) { this.variantSku = variantSku; }
    public void setVariantLabel(String variantLabel) { this.variantLabel = variantLabel; }
    public void setName(String name) { this.name = name; }
    public void setPrice(Double price) { this.price = price; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }

    public double getLineTotal() {
        return (price != null ? price : 0.0) * quantity;
    }

    public boolean isLowStock() {
        return availableStock > 0 && availableStock <= 3;
    }

    public String getAvailabilityLabel() {
        if (availableStock <= 0) {
            return "Check availability";
        }
        if (isLowStock()) {
            return "Only " + availableStock + " left";
        }
        return "In stock";
    }
}
