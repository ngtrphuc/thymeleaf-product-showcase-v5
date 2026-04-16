package io.github.ngtrphuc.smartphone_shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;

@Configuration
public class ThymeleafConfig {

    private final Environment env;

    public ThymeleafConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public SpringResourceTemplateResolver customerTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/customer/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        resolver.setCacheable(!env.matchesProfiles("dev"));
        return resolver;
    }

    @Bean
    public SpringResourceTemplateResolver adminTemplateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/admin/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        resolver.setOrder(2);
        resolver.setCheckExistence(true);
        resolver.setCacheable(!env.matchesProfiles("dev"));
        return resolver;
    }
}
