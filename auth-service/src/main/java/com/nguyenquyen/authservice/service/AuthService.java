package com.nguyenquyen.authservice.service;


import com.nguyenquyen.authservice.dto.request.*;
import com.nguyenquyen.authservice.dto.response.*;
import com.nguyenquyen.authservice.entity.RefreshToken;
import com.nguyenquyen.authservice.entity.User;
import com.nguyenquyen.authservice.enums.Role;
import com.nguyenquyen.authservice.event.UserRegisteredEvent;
import com.nguyenquyen.authservice.exception.*;
import com.nguyenquyen.authservice.repository.RefreshTokenRepository;
import com.nguyenquyen.authservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Auth Service — Core business logic
 *
 * Flows:
 * ──────
 * SIGNUP:      signup() → send OTP → user must call verifyOtp()
 * LOGIN:       login() → if verified → return tokens; if not → send OTP
 * VERIFY OTP:  validateOtp() → mark verified → publish Kafka event → return tokens
 * REFRESH:     refreshToken() → validate refresh token → rotate → return new tokens
 * LOGOUT:      logout() → revoke refresh token
 * GOOGLE:      googleAuth() → verify with Google → create/find user → return tokens
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final GoogleAuthService googleAuthService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_EVENTS_TOPIC = "user-events";


    @Transactional
    public String signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .emailVerified(false)
                .role(Role.USER)
                .build();

        userRepository.save(user);
        log.info("New user registered (unverified): {}", user.getEmail());

        // Generate OTP and send email
        String otp = otpService.generateAndStoreOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), otp);

        return "Registration successful! Please check your email for a 6-digit verification code.";
    }

    // ════════════════════════════════════════════════════════════
    // OTP VERIFICATION
    // ════════════════════════════════════════════════════════════

    /**
     * Step 2 of registration: Verify OTP → mark email verified → return tokens
     *
     * Also called after login if email not verified.
     */
    @Transactional
    public AuthResponse verifyOtp(OtpVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + request.getEmail()));

        if (user.isEmailVerified()) {
            throw new InvalidOtpException("Email is already verified. Please login.");
        }

        // Validate OTP (throws InvalidOtpException if wrong or expired)
        otpService.validateOtp(request.getEmail(), request.getOtp());

        // Mark email as verified
        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());

        // Publish Kafka event — User Profile Service will pick this up
        publishUserRegisteredEvent(user);

        return buildAuthResponse(user);
    }

    // ════════════════════════════════════════════════════════════
    // RESEND OTP
    // ════════════════════════════════════════════════════════════

    public String resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + request.getEmail()));

        if (user.isEmailVerified()) {
            throw new InvalidOtpException("Email is already verified. Please login.");
        }

        String otp = otpService.generateAndStoreOtp(user.getEmail());
        emailService.sendOtpEmail(user.getEmail(), otp);

        log.info("OTP resent for user: {}", user.getEmail());
        return "A new OTP has been sent to your email.";
    }

    // ════════════════════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new AuthException("Invalid email or password."));

        if (!user.isEnabled()) {
            throw new AuthException("Your account has been disabled. Please contact support.");
        }

        if (user.getPasswordHash() == null) {
            // This is a Google OAuth user — they don't have a password
            throw new AuthException("This account uses Google Sign-In. Please use 'Continue with Google'.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password.");
        }

        if (!user.isEmailVerified()) {
            // Resend OTP and tell frontend to show OTP screen
            String otp = otpService.generateAndStoreOtp(user.getEmail());
            emailService.sendOtpEmail(user.getEmail(), otp);
            throw new AuthException("Email not verified. A new OTP has been sent to your email.");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ════════════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ════════════════════════════════════════════════════════════

    /**
     * Token Rotation:
     * Old refresh token → revoke
     * New refresh token → create
     * New access token → return
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token."));

        if (!storedToken.isValid()) {
            // Token is revoked or expired — potential reuse attack
            // Revoke ALL tokens for this user (security measure)
            refreshTokenRepository.revokeAllUserTokens(storedToken.getUser());
            throw new InvalidTokenException("Refresh token is expired or revoked. Please login again.");
        }

        // Revoke old refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Generate new token pair
        User user = storedToken.getUser();
        return buildAuthResponse(user);
    }

    // ════════════════════════════════════════════════════════════
    // LOGOUT
    // ════════════════════════════════════════════════════════════

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("User logged out: {}", token.getUser().getEmail());
        });
    }

    // ════════════════════════════════════════════════════════════
    // GOOGLE OAUTH
    // ════════════════════════════════════════════════════════════

    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        // Verify token with Google
        GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.verifyToken(request.getIdToken());

        // Find existing user by googleId or email
        User user = userRepository.findByGoogleId(googleUser.googleId())
                .orElseGet(() -> userRepository.findByEmail(googleUser.email().toLowerCase())
                        .orElse(null));

        if (user == null) {
            // New user via Google — create account (already verified)
            user = User.builder()
                    .email(googleUser.email().toLowerCase())
                    .name(googleUser.name())
                    .googleId(googleUser.googleId())
                    .emailVerified(true)    // Google already verified their email
                    .role(Role.USER)
                    .build();
            userRepository.save(user);

            // Publish event for new Google users too
            publishUserRegisteredEvent(user);
            log.info("New user via Google OAuth: {}", user.getEmail());

        } else {
            // Existing user — link googleId if not already linked
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleUser.googleId());
                user.setEmailVerified(true);
                userRepository.save(user);
            }
        }

        if (!user.isEnabled()) {
            throw new AuthException("Your account has been disabled.");
        }

        return buildAuthResponse(user);
    }

    // ════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    /**
     * Build the full AuthResponse with access token + new refresh token
     */
    @Transactional
    protected AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        // Store refresh token in DB
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .revoked(false)
                .expiryDate(LocalDateTime.now().plusSeconds(
                        jwtService.getRefreshTokenExpirationMs() / 1000))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .emailVerified(user.isEmailVerified())
                        .build())
                .build();
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .eventType("USER_REGISTERED")
                    .userId(user.getId().toString())
                    .email(user.getEmail())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .registeredAt(user.getCreatedAt())
                    .build();

            kafkaTemplate.send(USER_EVENTS_TOPIC, user.getId().toString(), event);
            log.info("Published USER_REGISTERED event for userId: {}", user.getId());

        } catch (Exception e) {
            // Don't fail registration if Kafka is down — log and continue
            log.error("Failed to publish USER_REGISTERED event for userId: {}. Error: {}",
                    user.getId(), e.getMessage());
        }
    }
}
