package io.github.ngtrphuc.smartphone_shop.model;
public class CartItem {
    private Long id;
    private String name;
    private Double price;
    private int quantity;
    private String imageUrl;
    private int availableStock;
    public CartItem() {}
    public CartItem(Long id, String name, Double price, int quantity) {
        this(id, name, price, quantity, null, 0);
    }

    public CartItem(Long id, String name, Double price, int quantity, String imageUrl, int availableStock) {
        this.id = id;
        this.name = name;
        this.price = price != null ? price : 0.0;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.availableStock = availableStock;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getImageUrl() { return imageUrl; }
    public int getAvailableStock() { return availableStock; }
    public void setId(Long id) { this.id = id; }
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
