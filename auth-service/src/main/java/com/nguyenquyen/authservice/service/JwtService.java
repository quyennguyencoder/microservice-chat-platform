package com.nguyenquyen.authservice.service;

import com.nguyenquyen.authservice.entity.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Service — Token generation (Auth Service side)
 *
 * CONCEPT: Auth Service vs Gateway — JWT responsibilities
 * ────────────────────────────────────────────────────────
 * Auth Service (this):
 *   - GENERATES tokens (sign karta hai with secret)
 *   - Refresh token rotation
 *
 * API Gateway:
 *   - VALIDATES tokens (signature verify karta hai)
 *   - Extracts claims, adds X-User-* headers
 *
 * Same secret key dono jagah use hoti hai.
 * Secret = config-repo/application.yml → app.jwt.secret
 *
 * CONCEPT: JWT Structure
 * ──────────────────────
 * header.payload.signature
 *
 * Payload (claims) in our access token:
 *   sub   → userId (UUID as string)
 *   email → user email
 *   role  → USER or ADMIN
 *   iat   → issued at (auto by jjwt)
 *   exp   → expiry (auto by jjwt)
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:900000}")          // default 15 minutes
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-expiration:604800000}") // default 7 days
    private long refreshTokenExpiration;

    /**
     * Generate Access Token for a given User using Nimbus JWT
     *
     * CONCEPT: Claims
     * ────────────────
     * Claims = JWT payload me stored key-value pairs.
     * Standard claims: sub, iat, exp
     * Custom claims: email, role (hamne add kiye)
     *
     * Gateway inhe extract karke X-User-* headers me daalta hai.
     */
    public String generateAccessToken(User user) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + accessTokenExpiration);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())                  // "sub" claim
                    .claim("email", user.getEmail())
                    .claim("role", user.getRole().name())
                    .claim("name", user.getName())
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            signedJWT.sign(new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8)));

            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Error generating access token", e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    /**
     * Generate Refresh Token — just a random UUID, no user data
     *
     * CONCEPT: Why random UUID and not JWT for refresh token?
     * ─────────────────────────────────────────────────────────
     * JWT refresh token = stateless (like access token)
     *   Problem: Can't revoke individually without a blocklist
     *
     * Random UUID stored in DB = stateful
     *   Advantage: Can revoke specific sessions (logout)
     *   Can revoke all sessions (logout all devices)
     *   Can check if token was already used (rotation)
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Extract all claims from a token (for internal use — e.g., logout)
     */
    public JWTClaimsSet extractAllClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify the token signature
            if (!signedJWT.verify(new MACVerifier(jwtSecret.getBytes(StandardCharsets.UTF_8)))) {
                log.error("Invalid JWT signature");
                throw new RuntimeException("Invalid JWT signature");
            }

            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Failed to parse JWT token", e);
        } catch (JOSEException e) {
            log.error("Error verifying JWT token", e);
            throw new RuntimeException("Failed to verify JWT token", e);
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpiration;
    }
}
