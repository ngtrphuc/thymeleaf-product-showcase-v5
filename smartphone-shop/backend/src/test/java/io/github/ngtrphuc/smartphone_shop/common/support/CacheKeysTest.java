package io.github.ngtrphuc.smartphone_shop.common.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CacheKeysTest {

    @Test
    void catalog_shouldBuildStableHashedKey() {
        String key = CacheKeys.catalog(
                "iPhone",
                "price_desc",
                "Apple",
                "256",
                "over250",
                250001d,
                null,
                "over5000",
                5000,
                null,
                "6.5to6.8",
                9,
                2);

        assertTrue(key.startsWith("catalog:v1:"));
        assertEquals(75, key.length());
    }

    @Test
    void catalog_shouldDifferentiateDistinctFilterSets() {
        String keyA = CacheKeys.catalog("iphone", "name_asc", "", "", "", null, null, "", null, null, "", 9, 0);
        String keyB = CacheKeys.catalog("iphone", "name_asc", "", "", "", null, null, "", null, null, "", 9, 1);
        assertNotEquals(keyA, keyB);
    }

    @Test
    void catalog_shouldNotCollideWithDelimiterLikeInputs() {
        String keyA = CacheKeys.catalog("abc|brand=x", "name_asc", "", "", "", null, null, "", null, null, "", 9, 0);
        String keyB = CacheKeys.catalog("abc", "name_asc", "x", "", "", null, null, "", null, null, "", 9, 0);
        assertNotEquals(keyA, keyB);
    }
}
