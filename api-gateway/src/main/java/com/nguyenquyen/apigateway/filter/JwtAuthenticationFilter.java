package com.nguyenquyen.apigateway.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.regex.Pattern;


@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    
    private static final String AUTHORIZATION_HEADER = HttpHeaders.AUTHORIZATION;
    private static final String BEARER_PREFIX = "Bearer ";



    // ─── Public paths — JWT check skip karo ───
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/verify-otp",
            "/api/auth/resend-otp",
            "/api/auth/google",
            "/actuator",
            "/gateway",
            "/fallback",
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars"
    );


    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        log.debug("Processing request: {} {}", exchange.getRequest().getMethod(), path);

        // Step 1: Public path check
        if (isPublicPath(path)) {
            log.debug("Public path, skipping JWT check: {}", path);
            return chain.filter(exchange);
        }

        // Step 2: Authorization header extract
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return sendUnauthorizedResponse(exchange, "Missing Authorization header.");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            JWTClaimsSet claims = parseAndValidateToken(token);

            String userId = claims.getSubject();
            String userEmail = (String) claims.getClaim("email");
            String userRole = (String) claims.getClaim("role");

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Email", userEmail != null ? userEmail : "")
                    .header("X-User-Role", userRole != null ? userRole : "USER")
                    .header("X-Token-Validated", "true")
                    .build();

            log.debug("JWT validated. UserId={}", userId);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ParseException e) {
            log.warn("Failed to parse JWT for path: {}", path);
            return sendUnauthorizedResponse(exchange, "Invalid token format.");

        } catch (JOSEException e) {
            log.warn("JWT signature verification failed for path: {}", path);
            return sendUnauthorizedResponse(exchange, "Invalid token.");

        } catch (Exception e) {
            log.error("Unexpected error during JWT validation", e);
            return sendUnauthorizedResponse(exchange, "Authentication failed.");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicPath(String path) {
        boolean isStandardPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);

        return isStandardPublic;
    }

    private JWTClaimsSet parseAndValidateToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Verify signature with the JWT secret
        MACVerifier verifier = new MACVerifier(jwtSecret.getBytes(StandardCharsets.UTF_8));
        if (!signedJWT.verify(verifier)) {
            throw new JOSEException("JWT signature verification failed");
        }

        // Get claims
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

        // Check expiration
        if (claims.getExpirationTime() != null && 
            claims.getExpirationTime().before(new java.util.Date())) {
            throw new JOSEException("JWT token has expired");
        }

        return claims;
    }

    private Mono<Void> sendUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();

        String responseBody = """
                {"status":401,"error":"Unauthorized","message":"%s","path":"%s"}
                """.formatted(message, path).strip();

        DataBuffer buffer = response.bufferFactory()
                .wrap(responseBody.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}