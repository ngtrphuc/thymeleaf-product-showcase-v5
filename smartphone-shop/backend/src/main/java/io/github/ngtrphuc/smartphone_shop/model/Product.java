package io.github.ngtrphuc.smartphone_shop.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(length = 255)
    private String name;

    @Column(length = 180)
    private String slug;

    @Column(name = "sku_prefix", length = 40)
    private String skuPrefix;

    private Double price;

    @Column(name = "base_price")
    private Double basePrice;

    private String imageUrl;

    private Integer stock;

    @Column(nullable = false)
    private Boolean active = true;

    private String os;

    @Column(name = "Chipset")
    private String chipset;

    private String speed;
    private String ram;
    private String storage;
    private String size;
    private String resolution;
    private String battery;
    private String charging;

    @Column(length = 1000)
    private String description;

    @OneToMany(mappedBy = "product")
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductSpec> specs = new ArrayList<>();

    public Product() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Brand getBrand() { return brand; }
    public void setBrand(Brand brand) { this.brand = brand; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSkuPrefix() { return skuPrefix; }
    public void setSkuPrefix(String skuPrefix) { this.skuPrefix = skuPrefix; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public String getChipset() { return chipset; }
    public void setChipset(String chipset) { this.chipset = chipset; }

    public String getSpeed() { return speed; }
    public void setSpeed(String speed) { this.speed = speed; }

    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }

    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getBattery() { return battery; }
    public void setBattery(String battery) { this.battery = battery; }

    public String getCharging() { return charging; }
    public void setCharging(String charging) { this.charging = charging; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ProductVariant> getVariants() { return variants; }
    public void setVariants(List<ProductVariant> variants) { this.variants = variants; }

    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }

    public List<ProductSpec> getSpecs() { return specs; }
    public void setSpecs(List<ProductSpec> specs) { this.specs = specs; }

    public Double getEffectivePrice() {
        if (basePrice != null && basePrice >= 0) {
            return basePrice;
        }
        return price != null ? price : 0.0;
    }

    public int getEffectiveStock() {
        return stock != null ? Math.max(0, stock) : 0;
    }

    public String getBrandNameOrFallback() {
        if (brand != null && brand.getName() != null && !brand.getName().isBlank()) {
            return brand.getName();
        }
        String value = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("apple iphone") || value.startsWith("iphone")) {
            return "Apple";
        }
        if (value.startsWith("samsung") || value.startsWith("galaxy")) {
            return "Samsung";
        }
        if (value.startsWith("google") || value.startsWith("pixel")) {
            return "Google";
        }
        return "Other";
    }

    public boolean isAvailable() {
        return getEffectiveStock() > 0;
    }

    public boolean isLowStock() {
        int stockValue = getEffectiveStock();
        return stockValue > 0 && stockValue <= 3;
    }

    public String getAvailabilityLabel() {
        int stockValue = getEffectiveStock();
        if (stockValue <= 0) {
            return "Out of stock";
        }
        if (stockValue <= 3) {
            return "Only " + stockValue + " left";
        }
        return "In stock";
    }

    public long getMonthlyInstallmentAmount() {
        Double resolvedPrice = getEffectivePrice();
        if (resolvedPrice == null || resolvedPrice <= 0) {
            return 0L;
        }
        return Math.round(resolvedPrice / 24.0);
    }
}
