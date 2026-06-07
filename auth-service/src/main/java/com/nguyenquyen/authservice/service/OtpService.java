package com.nguyenquyen.authservice.service;

import com.nguyenquyen.authservice.exception.InvalidOtpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * OTP Service — Redis-backed OTP generation and validation
 *
 * CONCEPT: SecureRandom vs Random
 * ─────────────────────────────────
 * Random          → Predictable (seeded by time) — NOT safe for OTP
 * SecureRandom    → Cryptographically secure, OS entropy source — SAFE for OTP
 *
 * CONCEPT: Redis TTL for OTP
 * ───────────────────────────
 * Redis key: "otp:{email}"
 * Value:     "483921"  (6-digit string)
 * TTL:       300 seconds (5 minutes)
 *
 * After TTL → Redis automatically deletes the key.
 * OTP verification after 5 min → key not found → "OTP expired"
 *
 * CONCEPT: OTP Invalidation after use
 * ─────────────────────────────────────
 * After successful verification, immediately delete the OTP from Redis.
 * This prevents OTP reuse — each OTP can only be used once.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.otp.expiration:300}")
    private int otpExpirationSeconds;

    @Value("${app.otp.length:6}")
    private int otpLength;

    private static final String OTP_KEY_PREFIX = "otp:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generate and store OTP for given email.
     * Returns the OTP so EmailService can send it.
     */
    public String generateAndStoreOtp(String email) {
        String otp = generateOtp();
        String redisKey = OTP_KEY_PREFIX + email.toLowerCase();

        redisTemplate.opsForValue().set(redisKey, otp, otpExpirationSeconds, TimeUnit.SECONDS);
        log.debug("OTP stored for email: {} (expires in {}s)", email, otpExpirationSeconds);

        return otp;
    }

    /**
     * Validate OTP for given email.
     * Deletes OTP after successful validation (single-use).
     *
     * @throws InvalidOtpException if OTP is expired or incorrect
     */
    public void validateOtp(String email, String otp) {
        String redisKey = OTP_KEY_PREFIX + email.toLowerCase();
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new InvalidOtpException("OTP has expired. Please request a new one.");
        }

        if (!storedOtp.equals(otp)) {
            throw new InvalidOtpException("Invalid OTP. Please check and try again.");
        }

        // Delete OTP after successful use — prevents reuse
        redisTemplate.delete(redisKey);
        log.debug("OTP validated and deleted for email: {}", email);
    }

    private String generateOtp() {
        // Generate exactly otpLength digits
        int max = (int) Math.pow(10, otpLength);
        int min = (int) Math.pow(10, otpLength - 1);
        int otp = min + SECURE_RANDOM.nextInt(max - min);
        return String.valueOf(otp);
    }
}
