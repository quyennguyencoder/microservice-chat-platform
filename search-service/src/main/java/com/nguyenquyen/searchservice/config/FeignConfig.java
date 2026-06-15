package com.nguyenquyen.searchservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import com.nguyenquyen.searchservice.util.SecurityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Optional<Jwt> jwtOpt = SecurityUtils.getJwt();
            jwtOpt.ifPresent(jwt -> {
                requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
            });
        };
    }
}
