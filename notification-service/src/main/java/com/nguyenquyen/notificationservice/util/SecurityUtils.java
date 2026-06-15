package com.nguyenquyen.notificationservice.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import com.nguyenquyen.notificationservice.exception.UnauthorizedException;

import java.util.*;

@UtilityClass
@Slf4j
public class SecurityUtils {

    public Optional<String> getCurrentUserId() {
        return getJwt().map(Jwt::getSubject);
    }

    public String requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    public Set<String> getCurrentUserRoles() {
        return getJwt()
                .map(jwt -> {
                    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                    if (realmAccess != null && realmAccess.containsKey("roles")) {
                        return new HashSet<>((List<String>) realmAccess.get("roles"));
                    }
                    return Collections.<String>emptySet();
                })
                .orElse(Collections.emptySet());
    }

    public Optional<Jwt> getJwt() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(Jwt.class::isInstance)
                .map(Jwt.class::cast);
    }

    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !"anonymousUser".equals(auth.getPrincipal()));
    }

    public boolean isAuthenticated() {
        return getAuthentication().isPresent();
    }

    public boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }
}
