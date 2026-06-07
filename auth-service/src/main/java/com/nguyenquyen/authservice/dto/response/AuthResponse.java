package com.nguyenquyen.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth Response — returned after successful login/signup/google/refresh
 *
 * CONCEPT: Token Pair
 * ────────────────────
 * accessToken:  Short-lived (15 min) — sent in every API request header
 * refreshToken: Long-lived (7 days) — stored securely by client,
 *               used only to get new access token
 *
 * Frontend storage:
 *   accessToken  → memory (JS variable) — never localStorage (XSS risk)
 *   refreshToken → httpOnly cookie      — JS can't read (XSS safe)
 *
 * Or simpler: both in memory, refresh on every page load.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;     // Access token expiry in seconds

    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String name;
        private String role;
        private boolean emailVerified;
    }
}
