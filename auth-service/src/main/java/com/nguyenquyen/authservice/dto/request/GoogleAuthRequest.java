package com.nguyenquyen.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Google OAuth2 Request
 *
 * CONCEPT: Google Sign-In Flow
 * ─────────────────────────────
 * 1. Frontend: User clicks "Continue with Google"
 * 2. Google returns an idToken (JWT signed by Google)
 * 3. Frontend sends idToken to our backend
 * 4. Backend calls Google API to verify the token
 * 5. Backend extracts email/name/googleId from verified token
 * 6. Backend creates/finds user → returns our JWT
 *
 * We never see the user's Google password — Google handles that.
 * We only receive a verified identity claim from Google.
 */
@Data
public class GoogleAuthRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;
}
