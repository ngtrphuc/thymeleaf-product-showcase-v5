package io.github.ngtrphuc.smartphone_shop.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(UserDetailsService userDetailsService,
            LoginSuccessHandler loginSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/customer/css/**",
                        "/admin/css/**",
                        "/customer/js/**",
                        "/admin/js/**",
                        "/images/**",
                        "/fonts/**",
                        "/svg/**",
                        "/actuator/health",
                        "/favicon.ico").permitAll()
                .requestMatchers("/", "/product/**", "/register", "/login", "/error", "/admin/access-denied-admin", "/cart/**", "/compare/**").permitAll()
                .requestMatchers("/api/v1/products/**", "/api/v1/cart/**", "/api/v1/compare/**", "/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/profile/**", "/api/v1/payment-methods/**", "/api/v1/orders/**",
                        "/api/v1/wishlist/**", "/api/v1/chat/**").hasRole("USER")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/profile/**", "/my-orders/**", "/chat/**", "/wishlist/**").hasRole("USER")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                )
                .formLogin(form -> form
                .loginPage("/login")
                .successHandler(loginSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
                )
                .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
                )
                .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    if (isApiRequest(request.getRequestURI())) {
                        writeApiError(response, 401, "UNAUTHORIZED", "Authentication is required.");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                    if (isApiRequest(request.getRequestURI())) {
                        if (currentAuth == null
                                || currentAuth instanceof AnonymousAuthenticationToken
                                || !currentAuth.isAuthenticated()) {
                            writeApiError(response, 401, "UNAUTHORIZED", "Authentication is required.");
                        } else {
                            writeApiError(response, 403, "FORBIDDEN", "You do not have permission to access this resource.");
                        }
                        return;
                    }
                    if (currentAuth == null
                            || currentAuth instanceof AnonymousAuthenticationToken
                            || !currentAuth.isAuthenticated()) {
                        response.sendRedirect("/login");
                    } else if (currentAuth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                        response.sendRedirect("/admin/access-denied-admin");
                    } else {
                        response.sendRedirect("/");
                    }
                })
                )
                .headers(headers -> {
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self' data:; " +
                            "connect-src 'self'; " +
                            "object-src 'none'; " +
                            "base-uri 'self'; " +
                            "form-action 'self'; " +
                            "frame-ancestors 'none'"));
                    headers.contentTypeOptions(withDefaults());
                    headers.frameOptions(frame -> frame.deny());
                    headers.cacheControl(withDefaults());
                    headers.addHeaderWriter(
                            new StaticHeadersWriter("Permissions-Policy",
                                    "camera=(), microphone=(), geolocation=(), payment=()"));
                    headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                })
                .sessionManagement(session -> session
                .sessionFixation().changeSessionId()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                );

        return http.build();
    }

    private boolean isApiRequest(String requestUri) {
        return requestUri != null && requestUri.startsWith("/api/");
    }

    private void writeApiError(jakarta.servlet.http.HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + escapeJson(message) + "\"}");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
