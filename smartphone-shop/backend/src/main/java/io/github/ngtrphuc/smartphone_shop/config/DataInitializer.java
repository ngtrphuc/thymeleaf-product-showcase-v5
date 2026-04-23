package io.github.ngtrphuc.smartphone_shop.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

@Configuration
@Profile("dev")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String CATALOG_PUBLIC_CACHE = "catalogPublic";
    private static final String PRODUCT_DETAIL_PUBLIC_CACHE = "productDetailPublic";
    private static final String BRAND_LIST_CACHE = "brandList";

    @Bean
    public CommandLineRunner initDatabase(ProductRepository repository, CacheManager cacheManager) {
        return args -> {
            Map<String, Product> existingByName = new LinkedHashMap<>();
            for (Product product : repository.findAll()) {
                String key = canonicalProductKey(product.getName());
                if (!key.isBlank()) {
                    existingByName.putIfAbsent(key, product);
                }
            }

            int inserted = 0;
            int updated = 0;
            List<Product> toSave = new java.util.ArrayList<>();

            for (ProductSeed seed : seeds()) {
                Product product = existingByName.get(seed.key());
                boolean isNew = product == null;
                if (isNew) {
                    product = new Product();
                    applySeed(product, seed);
                    toSave.add(product);
                    inserted++;
                    continue;
                }

                if (needsUpdate(product, seed)) {
                    applySeed(product, seed);
                    toSave.add(product);
                    updated++;
                }
            }

            if (!toSave.isEmpty()) {
                repository.saveAll(toSave);
                clearStorefrontCache(cacheManager, CATALOG_PUBLIC_CACHE);
                clearStorefrontCache(cacheManager, PRODUCT_DETAIL_PUBLIC_CACHE);
                clearStorefrontCache(cacheManager, BRAND_LIST_CACHE);
            }
            log.info("Product catalog synced. Inserted: {}, Updated: {}", inserted, updated);
        };
    }

    private void clearStorefrontCache(CacheManager cacheManager, @NonNull String cacheName) {
        if (cacheManager == null) {
            return;
        }
        String resolvedCacheName = Objects.requireNonNull(cacheName, "Cache name must not be null.");
        Cache cache = cacheManager.getCache(resolvedCacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void applySeed(Product product, ProductSeed seed) {
        product.setName(seed.name());
        product.setPrice(seed.price());
        if (product.getStock() == null) {
            product.setStock(10);
        }
        product.setImageUrl(seed.imageUrl());
        product.setOs(seed.os());
        product.setRam(seed.ram());
        product.setChipset(seed.chipset());
        product.setSpeed(seed.speed());
        product.setStorage(seed.storage());
        product.setSize(seed.size());
        product.setResolution(seed.resolution());
        product.setBattery(seed.battery());
        product.setCharging(seed.charging());
        product.setDescription(seed.description());
    }

    private boolean needsUpdate(Product product, ProductSeed seed) {
        return !Objects.equals(product.getName(), seed.name())
                || !Objects.equals(product.getPrice(), seed.price())
                || product.getStock() == null
                || !Objects.equals(product.getImageUrl(), seed.imageUrl())
                || !Objects.equals(product.getOs(), seed.os())
                || !Objects.equals(product.getRam(), seed.ram())
                || !Objects.equals(product.getChipset(), seed.chipset())
                || !Objects.equals(product.getSpeed(), seed.speed())
                || !Objects.equals(product.getStorage(), seed.storage())
                || !Objects.equals(product.getSize(), seed.size())
                || !Objects.equals(product.getResolution(), seed.resolution())
                || !Objects.equals(product.getBattery(), seed.battery())
                || !Objects.equals(product.getCharging(), seed.charging())
                || !Objects.equals(product.getDescription(), seed.description());
    }

    private List<ProductSeed> seeds() {
        return List.of(
                new ProductSeed("Apple iPhone 17e", 134800.0, "/images/iphone17e.png",
                        "iOS 26", "8 GB", "Apple A19", "3.78 GHz", "512 GB", "6.1 inch",
                        "2532 x 1170", "4005 mAh", "20W",
                        "Compact iPhone with A19 chip, 48MP camera and durable Ceramic Shield 2 design."),
                new ProductSeed("Apple iPhone 17", 164800.0, "/images/iphone17.png",
                        "iOS 26", "8 GB", "Apple A19", "3.78 GHz", "512 GB", "6.3 inch",
                        "2622 x 1206", "3692 mAh", "40W",
                        "Balanced flagship iPhone featuring ProMotion display and advanced dual-camera system."),
                new ProductSeed("Apple iPhone Air", 229800.0, "/images/iphoneair.png",
                        "iOS 26", "12 GB", "Apple A19 Pro", "4.05 GHz", "1 TB", "6.5 inch",
                        "2736 x 1260", "3149 mAh", "40W",
                        "Ultra-thin premium iPhone delivering flagship performance in a sleek lightweight design."),
                new ProductSeed("Apple iPhone 17 Pro", 249800.0, "/images/iphone17pro.png",
                        "iOS 26", "12 GB", "Apple A19 Pro", "4.05 GHz", "1 TB", "6.3 inch",
                        "2622 x 1206", "4200 mAh", "40W",
                        "Professional-grade iPhone with triple 48MP cameras and powerful A19 Pro performance."),
                new ProductSeed("Apple iPhone 17 Pro Max", 329800.0, "/images/iphone17promax.png",
                        "iOS 26", "12 GB", "Apple A19 Pro", "4.05 GHz", "2 TB", "6.9 inch",
                        "2868 x 1320", "4832 mAh", "45W",
                        "Apple's most powerful flagship with a large 6.9-inch display and advanced triple-camera system."),
                new ProductSeed("Samsung Galaxy S25 Ultra", 189800.0, "/images/galaxy_s25.png",
                        "Android 15 (One UI 7)", "12 GB", "Snapdragon 8 Elite", "4.47 GHz", "512 GB", "6.9 inch",
                        "3120 x 1440", "5000 mAh", "45W",
                        "Previous-generation Samsung Ultra flagship with Snapdragon 8 Elite, a 200MP camera, S Pen support and a 6.9-inch QHD+ display."),
                new ProductSeed("Samsung Galaxy S26 Ultra", 299200.0, "/images/galaxy-s26-ultra.png",
                        "Android 16 (One UI 8)", "16 GB", "Snapdragon 8 Elite Gen 5", "4.47 GHz", "1 TB",
                        "6.9 inch", "3120 x 1440", "5000 mAh", "60W",
                        "Samsung's most advanced flagship featuring a 200MP camera, S Pen support and a premium 6.9-inch display."),
                new ProductSeed("Samsung Galaxy S26+", 196900.0, "/images/galaxy-s26-plus.png",
                        "Android 16 (One UI 8)", "12 GB", "Snapdragon 8 Elite Gen 5", "4.47 GHz", "512 GB",
                        "6.7 inch", "3120 x 1440", "4900 mAh", "45W",
                        "Large-screen Samsung flagship delivering powerful performance and an immersive AMOLED display."),
                new ProductSeed("Samsung Galaxy S26", 163900.0, "/images/galaxy-s26.png",
                        "Android 16 (One UI 8)", "12 GB", "Snapdragon 8 Elite Gen 5", "4.47 GHz", "512 GB",
                        "6.3 inch", "2340 x 1080", "4300 mAh", "45W",
                        "Compact Samsung flagship with top-tier performance and a high-quality AMOLED display."),
                new ProductSeed("Samsung Galaxy Z Fold7", 329320.0, "/images/galaxy-z-fold7.png",
                        "Android 16 (One UI 8)", "16 GB", "Snapdragon 8 Elite", "4.32 GHz", "1 TB", "8.0 inch",
                        "2184 x 1968", "4400 mAh", "25W",
                        "Premium foldable smartphone with a tablet-sized display and cutting-edge multitasking capabilities."),
                new ProductSeed("Xiaomi 17 Ultra", 219800.0, "/images/xiaomi17ultra.png",
                        "Android 16 (HyperOS 3)", "16 GB", "Snapdragon 8 Elite Gen 2", "4.47 GHz", "1 TB",
                        "6.73 inch", "3200 x 1440", "5600 mAh", "120W",
                        "Xiaomi's next-generation Ultra flagship featuring Leica optics, extreme performance and ultra-fast charging."),
                new ProductSeed("Xiaomi MIX Flip 2", 185000.0, "/images/xiaomi-mix-flip2.png",
                        "Android 15 (HyperOS 2)", "16 GB", "Snapdragon 8 Elite", "4.32 GHz", "1 TB",
                        "6.86 inch", "2912 x 1224", "5165 mAh", "67W",
                        "Stylish foldable flip smartphone combining flagship hardware with a compact design."),
                new ProductSeed("OPPO Find X9 Pro", 175000.0, "/images/findx9pro.png",
                        "Android 16 (ColorOS 16)", "16 GB", "Dimensity 9500", "4.21 GHz", "512 GB", "6.8 inch",
                        "2772 x 1272", "7500 mAh", "80W",
                        "Powerful OPPO flagship designed for photography and high-performance mobile computing."),
                new ProductSeed("OPPO Find N5", 304690.0, "/images/findn5.png",
                        "Android 15 (ColorOS)", "16 GB", "Snapdragon 8 Elite", "4.32 GHz", "512 GB", "8.12 inch",
                        "2480 x 2248", "5600 mAh", "80W",
                        "Premium foldable smartphone offering a large immersive display and flagship performance."),
                new ProductSeed("Sony Xperia 1 VII", 234300.0, "/images/xperia1vii.png",
                        "Android 15", "16 GB", "Snapdragon 8 Elite", "4.32 GHz", "512 GB", "6.5 inch",
                        "2340 x 1080", "5000 mAh", "30W",
                        "Sony's flagship smartphone built for creators with professional camera and display technology."),
                new ProductSeed("Sony Xperia 1 VI", 218900.0, "/images/xperia1vi.png",
                        "Android 14", "12 GB", "Snapdragon 8 Gen 3", "3.30 GHz", "512 GB", "6.5 inch",
                        "2340 x 1080", "5000 mAh", "30W",
                        "Premium Sony flagship combining cinematic display quality with advanced camera features."),
                new ProductSeed("Huawei Pura 70 Ultra", 189800.0, "/images/pura70ultra.png",
                        "HarmonyOS", "16 GB", "Kirin 9010", "2.30 GHz", "512 GB", "6.8 inch",
                        "2844 x 1260", "5200 mAh", "100W",
                        "Huawei's camera-focused flagship delivering cutting-edge photography performance."),
                new ProductSeed("Huawei Mate X7", 239800.0, "/images/huawei-mate-x7.png",
                        "HarmonyOS 6", "16 GB", "Kirin 9030 Pro", "3.2 GHz", "512 GB", "8.0 inch",
                        "2416 x 2210", "5600 mAh", "66W",
                        "Premium foldable flagship with Kirin 9030 Pro chipset, 8-inch LTPO OLED display and advanced triple camera system."),
                new ProductSeed("RedMagic 11 Pro 5G", 169800.0, "/images/redmagic11pro.png",
                        "Android 15 (RedMagic OS)", "16 GB", "Snapdragon 8 Elite", "3.3 GHz", "512 GB", "6.85 inch",
                        "2688 x 1216", "7500 mAh", "80W",
                        "Gaming flagship featuring Snapdragon 8 Elite, advanced cooling system and huge 7500 mAh battery for long gaming sessions."),
                new ProductSeed("Honor Magic V5", 219800.0, "/images/honor-magic-v5.png",
                        "Android 15 (MagicOS)", "16 GB", "Snapdragon 8 Elite", "3.3 GHz", "512 GB", "7.95 inch",
                        "2344 x 2156", "5820 mAh", "66W",
                        "Ultra thin foldable flagship with Snapdragon 8 Elite, large LTPO OLED display and powerful camera system."),
                new ProductSeed("HONOR 400 Pro 5G", 129800.0, "/images/honor400pro.png",
                        "Android 15 (MagicOS)", "12 GB", "Snapdragon 8 Gen 3", "3.3 GHz", "512 GB", "6.78 inch",
                        "2700 x 1224", "5200 mAh", "100W",
                        "High performance smartphone with Snapdragon 8 Gen 3 and ultra fast charging technology."),
                new ProductSeed("OPPO Find N6 5G", 219800.0, "/images/oppo-find-n6.png",
                        "Android 15 (ColorOS)", "16 GB", "Snapdragon 8 Elite", "3.3 GHz", "512 GB", "8.12 inch",
                        "2480 x 2200", "6000 mAh", "80W",
                        "Next generation OPPO foldable with Snapdragon 8 Elite and large LTPO OLED display."),
                new ProductSeed("Apple iPhone 16 Pro Max", 189800.0, "/images/iphone16promax.png",
                        "iOS 18", "8 GB", "Apple A18 Pro", "4.05 GHz", "512 GB", "6.9 inch",
                        "2868 x 1320", "4676 mAh", "27W",
                        "Apple flagship smartphone with A18 Pro chip, advanced camera system and large ProMotion OLED display."),
                new ProductSeed("Apple iPhone 16 Pro", 169800.0, "/images/iphone16pro.png",
                        "iOS 18", "8 GB", "Apple A18 Pro", "4.05 GHz", "512 GB", "6.3 inch",
                        "2622 x 1206", "3582 mAh", "27W",
                        "Professional iPhone model with powerful A18 Pro chip and premium titanium design."),
                new ProductSeed("Apple iPhone 16 Plus", 154800.0, "/images/iphone16plus.png",
                        "iOS 18", "8 GB", "Apple A18", "3.9 GHz", "512 GB", "6.7 inch",
                        "2796 x 1290", "4674 mAh", "25W",
                        "Large-screen iPhone with A18 chip, excellent battery life and advanced dual-camera system."),
                new ProductSeed("Samsung Galaxy Z Flip7", 159800.0, "/images/zflip7.png",
                        "Android 15 (One UI)", "12 GB", "Snapdragon 8 Elite", "3.3 GHz", "512 GB", "6.9 inch",
                        "2640 x 1080", "4300 mAh", "25W",
                        "Stylish foldable smartphone with upgraded hinge design and flagship Snapdragon performance."),
                new ProductSeed("ZTE Nubia Z70 Ultra", 149800.0, "/images/z70ultra.png",
                        "Android 15 (MyOS)", "16 GB", "Snapdragon 8 Elite", "3.3 GHz", "512 GB", "6.85 inch",
                        "2688 x 1216", "6150 mAh", "80W",
                        "Flagship smartphone with under-display camera, large battery and powerful gaming performance.")
        );
    }

    private static String canonicalProductKey(String name) {
        if (name == null) {
            return "";
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("apple iphone")) {
            return "iphone" + key.substring("apple iphone".length());
        }
        return key;
    }

    private record ProductSeed(
            String name,
            Double price,
            String imageUrl,
            String os,
            String ram,
            String chipset,
            String speed,
            String storage,
            String size,
            String resolution,
            String battery,
            String charging,
        String description) {

        String key() {
            return canonicalProductKey(name);
        }
    }
}
