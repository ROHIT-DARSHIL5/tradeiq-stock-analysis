package com.trading.engine.config;

// ============================================================
// CorsConfig.java — Cross-Origin Resource Sharing Configuration
//
// CORS is a browser security mechanism that blocks requests
// from one origin (e.g. file:///index.html) to a different
// origin (e.g. http://localhost:8080).
//
// This config allows the frontend HTML to call our REST API.
// ============================================================

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration  // Marks this class as a Spring configuration bean
public class CorsConfig {

    /**
     * corsConfigurer() — registers a WebMvcConfigurer bean that applies
     * CORS headers to every HTTP response from the /api/** endpoints.
     *
     * @return WebMvcConfigurer with CORS rules applied
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry
                    // Apply to all API endpoints
                    .addMapping("/api/**")
                    // Allow requests from any origin (frontend can be opened as a file)
                    .allowedOriginPatterns("*")
                    // Allow standard HTTP methods
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    // Allow all headers including Content-Type, Authorization
                    .allowedHeaders("*")
                    // Allow credentials (cookies, auth headers)
                    .allowCredentials(false)
                    // Cache preflight response for 1 hour (3600 seconds)
                    .maxAge(3600);
            }
        };
    }
}
