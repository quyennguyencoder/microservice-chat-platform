package com.nguyenquyen.searchservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import com.nguyenquyen.common.util.SecurityUtils;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

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
