package com.nguyenquyen.apigateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public GroupedOpenApi userServiceApi() {
        return GroupedOpenApi.builder()
                .group("user-service")
                .pathsToMatch("/user-service/**")
                .build();
    }

    @Bean
    public GroupedOpenApi chatServiceApi() {
        return GroupedOpenApi.builder()
                .group("chat-service")
                .pathsToMatch("/chat-service/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notificationServiceApi() {
        return GroupedOpenApi.builder()
                .group("notification-service")
                .pathsToMatch("/notification-service/**")
                .build();
    }

    @Bean
    public GroupedOpenApi searchServiceApi() {
        return GroupedOpenApi.builder()
                .group("search-service")
                .pathsToMatch("/search-service/**")
                .build();
    }

    @Bean
    public GroupedOpenApi storageServiceApi() {
        return GroupedOpenApi.builder()
                .group("storage-service")
                .pathsToMatch("/storage-service/**")
                .build();
    }
}
