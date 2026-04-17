package io.github.ngtrphuc.smartphone_shop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ApplicationPropertiesDefaultProfileTest {

    @Test
    void shouldDefaultToDevProfileForLocalRuns() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            assertNotNull(input, "application.properties must exist on classpath");
            properties.load(input);
        }

        assertEquals("dev", properties.getProperty("spring.profiles.default"));
    }
}
