package com.nguyenquyen.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;


@RestController
@Slf4j
@RequestMapping("/fallback")
public class FallbackController {

//    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/{serviceName}")
    public Mono<ResponseEntity<Map<String, Object>>> serviceFallback(
            @PathVariable String serviceName,
            ServerWebExchange exchange) {

        String requestPath = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        log.warn("Circuit breaker triggered for service: {} | Original path: {} {}",
                serviceName, method, requestPath);

        Map<String, Object> errorResponse = Map.of(
                "status", 503,
                "error", "Service Unavailable",
                "service", serviceName,
                "message", serviceName + " is temporarily unavailable. Please try again in a moment.",
                "timestamp", LocalDateTime.now().toString(),
                "path", requestPath
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(errorResponse));
    }


    @RequestMapping("/ws")
    public Mono<ResponseEntity<Map<String, Object>>> wsFallback(ServerWebExchange exchange) {
        log.warn("WebSocket fallback triggered");

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "Service Unavailable",
                        "service", "notification-service",
                        "message", "Real-time connection unavailable. Please refresh the page.",
                        "timestamp", LocalDateTime.now().toString()
                )));
    }
}