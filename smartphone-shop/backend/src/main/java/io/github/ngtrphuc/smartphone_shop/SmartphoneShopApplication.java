package io.github.ngtrphuc.smartphone_shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
public class SmartphoneShopApplication {

    public static void main(String[] args) {
        DevInfrastructureBootstrap.ensureStartedForDevIfNeeded(args);
        Port8080Guard.releaseForDevIfNeeded(args);
        DevFrontendBootstrap.ensureStartedForDevIfNeeded(args);
        SpringApplication.run(SmartphoneShopApplication.class, args);
    }
}
