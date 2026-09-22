package com.trading.engine;

// ============================================================
// TradingEngineApplication.java — Application Entry Point
//
// This is the main class that bootstraps the Spring Boot app.
// @SpringBootApplication is a convenience annotation that combines:
//   1. @Configuration     — marks class as a source of bean definitions
//   2. @EnableAutoConfiguration — auto-configures Spring based on classpath
//   3. @ComponentScan    — scans this package and sub-packages for components
// ============================================================

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TradingEngineApplication {

    /**
     * main() — JVM entry point.
     * SpringApplication.run() boots the embedded Tomcat server on port 8080
     * and registers all @RestController, @Service, @Component beans.
     */
    public static void main(String[] args) {
        SpringApplication.run(TradingEngineApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  Trading Strategy Engine is RUNNING");
        System.out.println("  API Base URL: http://localhost:8080/api");
        System.out.println("  Health Check: http://localhost:8080/api/health");
        System.out.println("==============================================");
    }
}
