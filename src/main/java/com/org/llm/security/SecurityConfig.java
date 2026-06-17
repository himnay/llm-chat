package com.org.llm.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * API-key based security for the REST API (servlet stack), modelled on {@code llm-gateway} /
 * {@code llm-rag-pipeline}: a stateless {@code X-API-Key} check with DB-backed keys, returning
 * {@code 401} JSON on failure.
 *
 * <p>Auth is <b>enabled by default</b>. Actuator, the demo static pages and {@code /error} stay
 * open; every other route requires a valid key. Set {@code app.security.auth-enabled=false} to
 * open everything for local development. Security response headers and CORS apply either way.</p>
 *
 * <p>Paths here are relative to the servlet context (the {@code server.servlet.context-path=/ai}
 * prefix is already stripped), e.g. {@code /chat}, {@code /audio/**}.</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    /** Open routes: actuator, the demo static UIs, Swagger/OpenAPI docs and infrastructure endpoints. */
    private static final String[] PUBLIC_PATHS = {
            "/actuator/**", "/error", "/favicon.ico",
            "/", "/index.html", "/*.html",
            "/css/**", "/js/**", "/images/**", "/webjars/**",
            "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
    };

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final SecurityProperties properties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)));

        // Rate limiting applies whether or not API-key auth is enabled.
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        if (!properties.isAuthEnabled()) {
            log.warn("SECURITY | API-key authentication is DISABLED (app.security.auth-enabled=false)");
            return http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).build();
        }

        log.info("SECURITY | API-key authentication is ENABLED (header={})", properties.getHeader());
        return http
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", properties.getHeader()));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
