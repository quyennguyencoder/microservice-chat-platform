package com.nguyenquyen.chatservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${openapi.gateway-url:http://localhost:8000}")
    private String gatewayUrl;

    @Value("${openapi.service-url:http://localhost:8020}")
    private String serviceUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chat Service")
                        .version("1.0")
                        .description("Chat Service API for ZChat"))
                .servers(List.of(
                        new Server().url(gatewayUrl).description("API Gateway (Cổng chính)"),
                        new Server().url(serviceUrl).description("Direct Service URL (Trực tiếp)")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập Access Token JWT từ Keycloak (Authorization: Bearer <token>)")));
    }
}