package io.github.ngtrphuc.smartphone_shop.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration(proxyBeanMethods = false)
public class WebPerformanceConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> productEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/v1/products", "/api/v1/products/*");
        registration.setName("productEtagFilter");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        return registration;
    }
}
