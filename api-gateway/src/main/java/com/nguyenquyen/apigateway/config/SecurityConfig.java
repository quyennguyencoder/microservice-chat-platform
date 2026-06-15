package com.nguyenquyen.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.*;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${cors.max-age}")
    private long maxAge;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_QUERY_PARAM = "token";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/pins/public/**").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("admin")
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/*/v3/api-docs/**").permitAll()
                        .pathMatchers("/ws/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenConverter(bearerTokenConverter())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                );

        return http.build();
    }

    @Bean
    public ServerAuthenticationConverter bearerTokenConverter() {

        return exchange -> {
            var request = exchange.getRequest();
            String path = request.getPath().value();

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length());
                return Mono.just(new BearerTokenAuthenticationToken(token));
            }

            String tokenParam = request.getQueryParams().getFirst(TOKEN_QUERY_PARAM);
            if (StringUtils.hasText(tokenParam)) {
                return Mono.just(new BearerTokenAuthenticationToken(tokenParam));
            }

            return Mono.empty();
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of(allowedMethods));
        configuration.setAllowedHeaders(List.of(allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public Converter<Jwt, Mono<JwtAuthenticationToken>> grantedAuthoritiesExtractor() {
        return jwt -> {

            Collection<GrantedAuthority> authorities = new ArrayList<>();

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                authorities.addAll(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .toList());
            }

            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                resourceAccess.forEach((resource, access) -> {
                    if (access instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> clientRoles = (Map<String, Object>) access;
                        if (clientRoles.containsKey("roles")) {
                            @SuppressWarnings("unchecked")
                            List<String> roles = (List<String>) clientRoles.get("roles");
                            authorities.addAll(roles.stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                    .toList());
                        }
                    }
                });
            }

            return Mono.just(new JwtAuthenticationToken(jwt, authorities));
        };
    }
}