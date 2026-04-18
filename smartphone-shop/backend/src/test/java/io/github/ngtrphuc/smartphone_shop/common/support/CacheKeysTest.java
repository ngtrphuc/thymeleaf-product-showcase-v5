package io.github.ngtrphuc.smartphone_shop.common.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class CacheKeysTest {

    @Test
    void catalog_shouldBuildStableReadableKey() {
        String key = CacheKeys.catalog(
                "iPhone",
                "price_desc",
                "Apple",
                "over250",
                250001d,
                null,
                "over5000",
                5000,
                null,
                "6.5to6.8",
                9,
                2);

        assertEquals(
                "catalog|keyword=iphone|sort=price_desc|brand=apple|priceRange=over250|priceMin=250001.0|priceMax=-|batteryRange=over5000|batteryMin=5000|batteryMax=-|screenSize=6.5to6.8|pageSize=9|page=2",
                key);
    }

    @Test
    void catalog_shouldDifferentiateDistinctFilterSets() {
        String keyA = CacheKeys.catalog("iphone", "name_asc", "", "", null, null, "", null, null, "", 9, 0);
        String keyB = CacheKeys.catalog("iphone", "name_asc", "", "", null, null, "", null, null, "", 9, 1);
        assertNotEquals(keyA, keyB);
    }
}
