package com.brokerx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("account-service", r -> r
                .path("/auth/**", "/users/**", "/accounts/**", "/audit/**")
                .uri("http://account-service:8081"))
            .route("wallet-service", r -> r
                .path("/wallets/**", "/transactions/**")
                .uri("http://wallet-service:8082"))
            .route("order-service", r -> r
                .path("/orders/**")
                .uri("http://order-service:8083"))
            .build();
    }
}