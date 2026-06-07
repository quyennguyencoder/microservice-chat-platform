package com.nguyenquyen.authservice.service;


import com.nguyenquyen.authservice.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Google Auth Service — Verify Google ID tokens
 *
 * CONCEPT: Google ID Token Verification
 * ───────────────────────────────────────
 * When user clicks "Sign in with Google":
 * 1. Frontend gets an idToken from Google (signed JWT)
 * 2. Frontend sends idToken to our backend
 * 3. We verify it by calling Google's tokeninfo endpoint
 * 4. Google returns user info if valid
 *
 * CONCEPT: Why call Google API instead of verifying JWT ourselves?
 * ─────────────────────────────────────────────────────────────────
 * Google's JWKS (JSON Web Key Set) rotates periodically.
 * Calling the tokeninfo endpoint is simpler and always up-to-date.
 * For high-traffic production: cache JWKS and verify locally.
 *
 * Google tokeninfo endpoint:
 * GET https://oauth2.googleapis.com/tokeninfo?id_token={token}
 *
 * Returns:
 * {
 *   "sub": "10769150350006150715113082367",  ← Google's user ID
 *   "email": "user@gmail.com",
 *   "email_verified": "true",
 *   "name": "User Name",
 *   "aud": "your-client-id.apps.googleusercontent.com"
 * }
 */
@Service
@Slf4j
public class GoogleAuthService {

    private static final String GOOGLE_TOKEN_INFO_URL =
            "https://oauth2.googleapis.com/tokeninfo";

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleUserInfo verifyToken(String idToken) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(GOOGLE_TOKEN_INFO_URL)
                    .queryParam("id_token", idToken)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                throw new AuthException("Failed to verify Google token");
            }

            // Check email is verified by Google
            String emailVerified = (String) response.get("email_verified");
            if (!"true".equals(emailVerified)) {
                throw new AuthException("Google account email is not verified");
            }

            String googleId = (String) response.get("sub");
            String email    = (String) response.get("email");
            String name     = (String) response.get("name");

            if (googleId == null || email == null) {
                throw new AuthException("Invalid Google token — missing required fields");
            }

            log.debug("Google token verified for email: {}", email);
            return new GoogleUserInfo(googleId, email, name != null ? name : email);

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
            throw new AuthException("Google authentication failed. Please try again.");
        }
    }

    public record GoogleUserInfo(String googleId, String email, String name) {}
}
