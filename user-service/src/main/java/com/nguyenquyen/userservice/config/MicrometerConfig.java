package com.nguyenquyen.userservice.config;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderThreadLocalAccessor;

@Configuration
public class MicrometerConfig {

    @PostConstruct
    public void init() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new SecurityContextHolderThreadLocalAccessor());
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
