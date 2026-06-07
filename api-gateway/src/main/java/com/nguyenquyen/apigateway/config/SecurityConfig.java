package com.nguyenquyen.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF disable — Gateway is stateless REST + service-to-service
                // No browser forms, no sessions → CSRF not applicable
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Allow all requests — JWT auth handled by JwtAuthenticationFilter GlobalFilter
                .authorizeExchange(auth -> auth
                        .anyExchange().permitAll()
                )

                // Disable form login — API-only, no HTML forms
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Disable HTTP Basic — JWT Bearer token is used instead
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .build();
    }
}
