package io.github.ngtrphuc.smartphone_shop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 80, unique = true)
    private String sku;

    @Column(length = 80)
    private String color;

    @Column(length = 80)
    private String storage;

    @Column(length = 80)
    private String ram;

    @Column(name = "price_override")
    private Double priceOverride;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram;
    }

    public Double getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(Double priceOverride) {
        this.priceOverride = priceOverride;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Double effectivePrice() {
        if (priceOverride != null && priceOverride >= 0) {
            return priceOverride;
        }
        return product != null ? product.getEffectivePrice() : null;
    }

    public String label() {
        String left = color == null ? "" : color.trim();
        String right = storage == null ? "" : storage.trim();
        if (!left.isEmpty() && !right.isEmpty()) {
            return left + " / " + right;
        }
        if (!left.isEmpty()) {
            return left;
        }
        if (!right.isEmpty()) {
            return right;
        }
        return "Default";
    }
}
