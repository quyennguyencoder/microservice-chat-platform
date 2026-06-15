package com.nguyenquyen.notificationservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TokenQueryParamFilter extends OncePerRequestFilter {

    private static final String TOKEN_PARAM = "token";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getParameter(TOKEN_PARAM);
        if (StringUtils.hasText(token)) {
            logger.debug("Token found in query param, wrapping request with Authorization header");
            HttpServletRequest wrappedRequest = new AuthorizationHeaderRequestWrapper(request, token);
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class AuthorizationHeaderRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> customHeaders = new HashMap<>();

        public AuthorizationHeaderRequestWrapper(HttpServletRequest request, String token) {
            super(request);
            this.customHeaders.put(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
        }

        @Override
        public String getHeader(String name) {
            String customValue = customHeaders.get(name);
            if (customValue != null) {
                return customValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>(customHeaders.keySet());
            Enumeration<String> parentNames = super.getHeaderNames();
            while (parentNames.hasMoreElements()) {
                names.add(parentNames.nextElement());
            }
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (customHeaders.containsKey(name)) {
                return Collections.enumeration(Collections.singletonList(customHeaders.get(name)));
            }
            return super.getHeaders(name);
        }
    }
}