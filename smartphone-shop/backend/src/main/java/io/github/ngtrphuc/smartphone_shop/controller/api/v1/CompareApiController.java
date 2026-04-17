package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.CompareService;

@RestController
@RequestMapping("/api/v1/compare")
public class CompareApiController {

    private final ProductRepository productRepository;
    private final CompareService compareService;
    private final ApiMapper apiMapper;

    public CompareApiController(ProductRepository productRepository,
            CompareService compareService,
            ApiMapper apiMapper) {
        this.productRepository = productRepository;
        this.compareService = compareService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public CompareResponse compare(Authentication authentication) {
        return currentCompare(authentication);
    }

    @PostMapping("/items")
    public CompareResponse add(@RequestBody CompareAddRequest request,
            Authentication authentication) {
        Long productId = request.productId();
        if (productId == null) {
            throw new IllegalArgumentException("Product ID is required.");
        }
        CompareService.AddResult result = compareService.addItem(resolveEmail(authentication), null, productId);
        if (result == CompareService.AddResult.UNAVAILABLE) {
            throw new NoSuchElementException("Product not found.");
        }
        if (result == CompareService.AddResult.LIMIT_REACHED) {
            throw new IllegalStateException("You can compare up to " + compareService.getMaxCompare() + " products.");
        }
        if (result == CompareService.AddResult.ALREADY_EXISTS) {
            throw new IllegalStateException("This product is already in compare list.");
        }
        return currentCompare(authentication);
    }

    @DeleteMapping("/items/{id}")
    public CompareResponse remove(@PathVariable(name = "id") long id, Authentication authentication) {
        compareService.removeItem(resolveEmail(authentication), null, id);
        return currentCompare(authentication);
    }

    @DeleteMapping
    public CompareResponse clear(Authentication authentication) {
        compareService.clear(resolveEmail(authentication), null);
        return currentCompare(authentication);
    }

    private CompareResponse currentCompare(Authentication authentication) {
        List<Long> ids = compareService.getCompareIds(resolveEmail(authentication), null);
        List<Product> products = resolveOrderedProducts(ids);
        return new CompareResponse(
                products.stream().map(product -> apiMapper.toProductSummary(product, false)).toList(),
                ids,
                compareService.getMaxCompare());
    }

    private List<Product> resolveOrderedProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllByIdIn(ids);
        Map<Long, Product> productsById = new HashMap<>();
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                productsById.put(product.getId(), product);
            }
        }
        return ids.stream()
                .map(productsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }

    private record CompareAddRequest(Long productId) {
    }
}

