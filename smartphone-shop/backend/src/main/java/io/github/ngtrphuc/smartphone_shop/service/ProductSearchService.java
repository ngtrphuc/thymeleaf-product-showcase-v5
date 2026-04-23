package io.github.ngtrphuc.smartphone_shop.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.ngtrphuc.smartphone_shop.common.support.StorefrontSupport;
import io.github.ngtrphuc.smartphone_shop.config.ProductSearchProperties;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

@Service
@EnableConfigurationProperties(ProductSearchProperties.class)
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);
    private static final int SYNC_BATCH_SIZE = 200;

    private final ProductSearchProperties properties;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile boolean indexEnsured;

    public ProductSearchService(ProductSearchProperties properties,
            ProductRepository productRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(250, properties.getConnectTimeoutMillis())))
                .build();
    }

    public ProductSearchResult searchProductIds(String keyword, int limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!properties.isEnabled() || normalizedKeyword.isBlank()) {
            return ProductSearchResult.notHandled();
        }
        try {
            ensureIndex();
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("q", normalizedKeyword);
            payload.put("limit", Math.max(1, Math.min(limit, properties.getSearchLimit())));
            ArrayNode facets = payload.putArray("facets");
            facets.add("brand");
            facets.add("storage");
            facets.add("os");
            ArrayNode attributesToRetrieve = payload.putArray("attributesToRetrieve");
            attributesToRetrieve.add("id");
            JsonNode root = sendJsonRequest("POST", indexPath() + "/search", payload);
            ArrayNode hits = root != null && root.has("hits") && root.get("hits").isArray()
                    ? (ArrayNode) root.get("hits")
                    : objectMapper.createArrayNode();

            LinkedHashSet<Long> ids = new LinkedHashSet<>();
            for (JsonNode hit : hits) {
                if (hit == null || !hit.hasNonNull("id")) {
                    continue;
                }
                ids.add(hit.get("id").asLong());
            }
            return ProductSearchResult.handled(new ArrayList<>(ids));
        } catch (Exception ex) {
            log.warn("Meilisearch query failed. Falling back to database keyword search. Cause: {}",
                    rootCauseMessage(ex));
            return ProductSearchResult.notHandled();
        }
    }

    public void syncProduct(Product product) {
        if (!properties.isEnabled() || product == null || product.getId() == null) {
            return;
        }
        try {
            ensureIndex();
            ArrayNode documents = objectMapper.createArrayNode();
            documents.add(toDocument(product));
            sendJsonRequest("POST", indexPath() + "/documents?primaryKey=id", documents);
        } catch (Exception ex) {
            log.warn("Failed to sync product {} to Meilisearch. Cause: {}", product.getId(), rootCauseMessage(ex));
        }
    }

    public void deleteProduct(long productId) {
        if (!properties.isEnabled() || productId <= 0) {
            return;
        }
        try {
            ensureIndex();
            sendJsonRequest("DELETE", indexPath() + "/documents/" + productId, null);
        } catch (Exception ex) {
            log.warn("Failed to delete product {} from Meilisearch. Cause: {}", productId, rootCauseMessage(ex));
        }
    }

    public void syncCatalog() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            ensureIndex();
            int syncedCount = 0;
            int pageNumber = 0;
            Page<Product> page;
            do {
                page = productRepository.findAll(PageRequest.of(
                        pageNumber,
                        SYNC_BATCH_SIZE,
                        Sort.by(Sort.Order.asc("id"))));
                if (!page.hasContent()) {
                    break;
                }
                ArrayNode documents = objectMapper.createArrayNode();
                for (Product product : page.getContent()) {
                    if (product != null && product.getId() != null) {
                        documents.add(toDocument(product));
                    }
                }
                if (!documents.isEmpty()) {
                    sendJsonRequest("POST", indexPath() + "/documents?primaryKey=id", documents);
                    syncedCount += documents.size();
                }
                pageNumber++;
            } while (page.hasNext());
            log.info("Synchronized {} products to Meilisearch index '{}' in batched mode.",
                    syncedCount,
                    properties.getIndexName());
        } catch (Exception ex) {
            log.warn("Failed to synchronize product catalog to Meilisearch. Cause: {}", rootCauseMessage(ex));
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncCatalogOnStartup() {
        if (properties.isEnabled() && properties.isSyncOnStartup()) {
            syncCatalog();
        }
    }

    private void ensureIndex() throws IOException, InterruptedException {
        if (indexEnsured) {
            return;
        }
        synchronized (this) {
            if (indexEnsured) {
                return;
            }
            ObjectNode createIndexPayload = objectMapper.createObjectNode();
            createIndexPayload.put("uid", properties.getIndexName());
            createIndexPayload.put("primaryKey", "id");
            sendJsonRequest("POST", "/indexes", createIndexPayload, 200, 201, 202, 409);

            ObjectNode settings = objectMapper.createObjectNode();
            ArrayNode searchable = settings.putArray("searchableAttributes");
            searchable.add("name");
            searchable.add("brand");
            searchable.add("os");
            searchable.add("chipset");
            searchable.add("storage");
            searchable.add("description");

            ArrayNode filterable = settings.putArray("filterableAttributes");
            filterable.add("brand");
            filterable.add("storage");
            filterable.add("os");
            filterable.add("price");
            filterable.add("stock");

            ArrayNode sortable = settings.putArray("sortableAttributes");
            sortable.add("price");
            sortable.add("name");
            sortable.add("stock");

            ArrayNode rankingRules = settings.putArray("rankingRules");
            rankingRules.add("words");
            rankingRules.add("typo");
            rankingRules.add("proximity");
            rankingRules.add("attribute");
            rankingRules.add("sort");
            rankingRules.add("exactness");

            ObjectNode faceting = settings.putObject("faceting");
            faceting.put("maxValuesPerFacet", 40);
            ObjectNode facetSort = faceting.putObject("sortFacetValuesBy");
            facetSort.put("brand", "alpha");
            facetSort.put("storage", "alpha");
            facetSort.put("os", "alpha");
            facetSort.put("*", "count");

            ObjectNode typoTolerance = settings.putObject("typoTolerance");
            typoTolerance.put("enabled", true);
            ObjectNode minWordSizeForTypos = typoTolerance.putObject("minWordSizeForTypos");
            minWordSizeForTypos.put("oneTypo", 4);
            minWordSizeForTypos.put("twoTypos", 8);

            sendJsonRequest("PATCH", indexPath() + "/settings", settings, 200, 202);
            indexEnsured = true;
        }
    }

    private ObjectNode toDocument(Product product) {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("id", product.getId());
        document.put("name", safeText(product.getName()));
        document.put("brand", StorefrontSupport.extractBrand(product.getName()));
        document.put("description", safeText(product.getDescription()));
        document.put("os", safeText(product.getOs()));
        document.put("chipset", safeText(product.getChipset()));
        document.put("storage", safeText(product.getStorage()));
        if (product.getPrice() != null) {
            document.put("price", product.getPrice());
        } else {
            document.putNull("price");
        }
        if (product.getStock() != null) {
            document.put("stock", product.getStock());
        } else {
            document.putNull("stock");
        }
        return document;
    }

    private JsonNode sendJsonRequest(String method, String path, JsonNode body, int... acceptableStatuses)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(properties.getHost()) + path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json");

        String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
        if (!apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request;
        if (body == null) {
            request = builder.method(method, HttpRequest.BodyPublishers.noBody()).build();
        } else {
            request = builder.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body))).build();
        }

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        for (int acceptableStatus : acceptableStatuses) {
            if (status == acceptableStatus) {
                String responseBody = response.body();
                if (responseBody == null || responseBody.isBlank()) {
                    return objectMapper.createObjectNode();
                }
                return objectMapper.readTree(responseBody);
            }
        }
        throw new IOException("Unexpected Meilisearch status " + status + " for " + method + " " + path);
    }

    private JsonNode sendJsonRequest(String method, String path, JsonNode body)
            throws IOException, InterruptedException {
        return sendJsonRequest(method, path, body, 200, 201, 202);
    }

    private String indexPath() {
        return "/indexes/" + properties.getIndexName().trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = Objects.requireNonNullElse(baseUrl, "http://localhost:7700").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String rootCauseMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    public record ProductSearchResult(boolean handled, List<Long> orderedIds) {
        public static ProductSearchResult notHandled() {
            return new ProductSearchResult(false, List.of());
        }

        public static ProductSearchResult handled(List<Long> orderedIds) {
            return new ProductSearchResult(true, orderedIds == null ? List.of() : List.copyOf(orderedIds));
        }
    }
}
