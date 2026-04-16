package io.github.ngtrphuc.smartphone_shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations(
                        "classpath:/frontend/static/customer/css/",
                        "classpath:/frontend/static/admin/css/"
                );
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/frontend/static/customer/images/");
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/frontend/static/customer/fonts/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/frontend/static/js/");
        registry.addResourceHandler("/svg/**")
                .addResourceLocations("classpath:/frontend/static/svg/");
    }
}
