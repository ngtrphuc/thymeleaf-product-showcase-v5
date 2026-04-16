package io.github.ngtrphuc.smartphone_shop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double price;
    private String imageUrl;
    private Integer stock;
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

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
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

    public boolean isAvailable() {
        return stock != null && stock > 0;
    }

    public boolean isLowStock() {
        return stock != null && stock > 0 && stock <= 3;
    }

    public String getAvailabilityLabel() {
        if (!isAvailable()) {
            return "Out of stock";
        }
        if (isLowStock()) {
            return "Only " + stock + " left";
        }
        return "In stock";
    }

    public long getMonthlyInstallmentAmount() {
        if (price == null || price <= 0) {
            return 0L;
        }
        return Math.round(price / 24.0);
    }
}
