package io.github.ngtrphuc.smartphone_shop;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class SmartphoneShopApplication {
    public static void main(String[] args) {
        DevInfrastructureBootstrap.ensureStartedForDevIfNeeded(args);
        Port8080Guard.releaseForDevIfNeeded(args);
        SpringApplication.run(SmartphoneShopApplication.class, args);
    }
}
