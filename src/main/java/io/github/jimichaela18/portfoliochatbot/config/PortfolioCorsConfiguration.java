package io.github.jimichaela18.portfoliochatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class PortfolioCorsConfiguration implements WebMvcConfigurer {

    private final String allowedOrigin;
    private final String[] allowedMethods;
    private final String[] allowedHeaders;
    private final long maxAgeSeconds;

    public PortfolioCorsConfiguration(
            @Value("${portfolio.cors.allowed-origin}") String allowedOrigin,
            @Value("${portfolio.cors.allowed-methods}") String allowedMethods,
            @Value("${portfolio.cors.allowed-headers}") String allowedHeaders,
            @Value("${portfolio.cors.max-age-seconds}") long maxAgeSeconds
    ) {
        this.allowedOrigin = allowedOrigin;
        this.allowedMethods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        this.allowedHeaders = Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        this.maxAgeSeconds = maxAgeSeconds;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/ask")
                .allowedOrigins(allowedOrigin)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .maxAge(maxAgeSeconds);
    }
}
