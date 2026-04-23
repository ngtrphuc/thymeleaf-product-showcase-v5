package io.github.ngtrphuc.smartphone_shop.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.spec.ProductCatalogSpecifications;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductCatalogSpecificationIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("smartphone_shop_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    void forCatalog_shouldFilterBrandBatteryAndScreenSizeOnDatabase() {
        productRepository.saveAll(Objects.requireNonNull(List.of(
                newProduct("Apple iPhone 15 Pro Max", 31_000_000d, "5000 mAh", "6.7\""),
                newProduct("Samsung Galaxy S24", 22_000_000d, "4900 mAh", "6.2\""),
                newProduct("Sony Xperia 1 VI", 28_000_000d, "5100 mAh", "6.9\""),
                newProduct("Nokia X50", 9_000_000d, "4300 mAh", "6.4\""))));

        Page<Product> page = productRepository.findAll(
                ProductCatalogSpecifications.forCatalog(
                        null,
                        null,
                        null,
                        null,
                        "apple",
                        null,
                        "5000to5499",
                        null,
                        null,
                        "6.7to6.8"),
                PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name").ignoreCase())));

        assertEquals(1, page.getTotalElements());
        assertEquals("Apple iPhone 15 Pro Max", page.getContent().getFirst().getName());
    }

    private Product newProduct(String name, double price, String battery, String size) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setBattery(battery);
        product.setSize(size);
        product.setStock(20);
        product.setImageUrl("/images/" + name.replace(' ', '-').toLowerCase() + ".png");
        return product;
    }
}
