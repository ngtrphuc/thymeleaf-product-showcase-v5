package io.github.ngtrphuc.smartphone_shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/customer/css/**")
                .addResourceLocations("classpath:/static/customer/css/");
        registry.addResourceHandler("/admin/css/**")
                .addResourceLocations("classpath:/static/admin/css/");

        registry.addResourceHandler("/customer/js/**")
                .addResourceLocations("classpath:/static/customer/js/");
        registry.addResourceHandler("/admin/js/**")
                .addResourceLocations("classpath:/static/admin/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/customer/images/");
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/customer/fonts/");
        registry.addResourceHandler("/svg/**")
                .addResourceLocations("classpath:/static/svg/");
    }
}
