package com.nguyenquyen.userservice.service;

import com.nguyenquyen.userservice.dto.request.UpdateProfileRequest;
import com.nguyenquyen.userservice.dto.response.UserProfileResponse;
import com.nguyenquyen.userservice.enums.OnlineStatus;
import com.nguyenquyen.userservice.entity.UserProfile;
import com.nguyenquyen.userservice.event.UserRegisteredEvent;
import com.nguyenquyen.userservice.exception.ProfileNotFoundException;
import com.nguyenquyen.userservice.exception.UsernameAlreadyTakenException;
import com.nguyenquyen.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/*
 *   createProfile()     → Called by Kafka consumer on USER_REGISTERED event
 *   getProfileById()    → Cached in Redis — used by other services too
 *   updateProfile()     → User updates own profile, evicts Redis cache
 *   searchUsers()       → Search by name/username/email (paginated)
 *   updateOnlineStatus()→ Called when user connects/disconnects WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private static final String PROFILES_CACHE = "profiles";

    private final UserProfileRepository profileRepository;

    // ════════════════════════════════════════════════════════════
    // CREATE — Called by Kafka consumer
    // ════════════════════════════════════════════════════════════


    @Transactional
    public void createProfileFromEvent(UserRegisteredEvent event) {
        UUID userId = UUID.fromString(event.getUserId());

        // Idempotency — Kafka might deliver the same event twice (at-least-once)
        if (profileRepository.existsById(userId)) {
            log.warn("Profile already exists for userId: {} — skipping duplicate event", userId);
            return;
        }

        String username = generateUniqueUsername(event.getEmail(), event.getName());

        UserProfile profile = UserProfile.builder()
                .id(userId)                             // Same UUID as auth service
                .displayName(event.getName())
                .username(username)
                .email(event.getEmail().toLowerCase())
                .phone(event.getPhone())
                .onlineStatus(OnlineStatus.OFFLINE)
                .build();

        profileRepository.save(profile);
        log.info("✅ Created profile for userId: {} with username: @{}", userId, username);
    }

    // ════════════════════════════════════════════════════════════
    // READ
    // ════════════════════════════════════════════════════════════

    /**
     * Get profile by userId — CACHED in Redis.
     *
     * CONCEPT: @Cacheable
     * ─────────────────────
     * Key: "profiles::<userId>"
     * Hit:  Return from Redis (< 1ms)
     * Miss: Fetch from DB → store in Redis → return
     * TTL:  30 minutes (configured in RedisConfig)
     *
     * @param userId the user's UUID (from X-User-Id header or path variable)
     */
    @Cacheable(value = PROFILES_CACHE, key = "#userId")
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(String userId) {
        log.debug("Cache miss for userId: {} — fetching from DB", userId);
        UserProfile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ProfileNotFoundException(userId));
        return UserProfileResponse.from(profile);
    }

    /**
     * Get profile by username (@handle)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username) {
        UserProfile profile = profileRepository.findByUsername(username)
                .orElseThrow(() -> new ProfileNotFoundException("@" + username));
        return UserProfileResponse.from(profile);
    }

    /**
     * Search profiles by query string (name, username, email)
     * Returns paginated results.
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50)); // max 50 per page
        return profileRepository.searchByQuery(query.trim(), pageable)
                .map(UserProfileResponse::from);
    }

    // ════════════════════════════════════════════════════════════
    // UPDATE
    // ════════════════════════════════════════════════════════════

    /**
     * Update profile fields — evicts cache on success.
     *
     * CONCEPT: @CacheEvict
     * ──────────────────────
     * After update, the cached version is stale.
     * @CacheEvict deletes the Redis entry → next read fetches fresh from DB.
     *
     * @param userId the userId from X-User-Id header (authenticated user)
     * @param request the fields to update (null = keep existing)
     */
    @CacheEvict(value = PROFILES_CACHE, key = "#userId")
    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserProfile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ProfileNotFoundException(userId));

        // Handle username change — must be unique
        if (request.getUsername() != null && !request.getUsername().equals(profile.getUsername())) {
            if (profileRepository.existsByUsername(request.getUsername())) {
                throw new UsernameAlreadyTakenException(request.getUsername());
            }
            profile.setUsername(request.getUsername());
        }

        // Partial update — only set non-null fields
        if (request.getDisplayName()  != null) profile.setDisplayName(request.getDisplayName());
        if (request.getBio()          != null) profile.setBio(request.getBio());
        if (request.getAvatarUrl()    != null) profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getPhone()        != null) profile.setPhone(request.getPhone());
        if (request.getDateOfBirth()  != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getLocation()     != null) profile.setLocation(request.getLocation());
        if (request.getWebsite()      != null) profile.setWebsite(request.getWebsite());
        if (request.getOnlineStatus() != null) profile.setOnlineStatus(request.getOnlineStatus());

        UserProfile saved = profileRepository.save(profile);
        log.info("Profile updated for userId: {}", userId);
        return UserProfileResponse.from(saved);
    }

    /**
     * Update online status — lightweight operation (no full profile update).
     * Also evicts cache so other services see fresh status.
     */
    @CacheEvict(value = PROFILES_CACHE, key = "#userId")
    @Transactional
    public void updateOnlineStatus(String userId, OnlineStatus status) {
        UserProfile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ProfileNotFoundException(userId));

        profile.setOnlineStatus(status);

        // Record lastSeen when going offline
        if (status == OnlineStatus.OFFLINE) {
            profile.setLastSeen(Instant.now());
        }

        profileRepository.save(profile);
        log.debug("Online status updated for userId: {} → {}", userId, status);
    }

    // ════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════

    /**
     * Generate a unique username from email prefix.
     *
     * Strategy:
     *   1. Take email prefix (before @): "aniket.kamlesh@gmail.com" → "aniket.kamlesh"
     *   2. Remove special chars except dots/underscores: "aniket.kamlesh"
     *   3. If taken, append random 4-digit suffix: "aniket.kamlesh1234"
     *
     * @param email user's email address
     * @param name  user's full name (fallback if email prefix is empty)
     */
    private String generateUniqueUsername(String email, String name) {
        // Step 1: derive base from email prefix
        String emailPrefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String base = emailPrefix
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "")    // remove unsupported chars
                .replaceAll("^[._-]+", "");          // strip leading dots/underscores

        // Fallback to name if email prefix produces empty/short string
        if (base.length() < 3) {
            base = name.toLowerCase().replaceAll("[^a-z0-9]", "").substring(0, Math.min(name.length(), 20));
        }

        // Ensure minimum length
        if (base.length() < 3) {
            base = "user";
        }

        // Step 2: check uniqueness, append suffix if needed
        String candidate = base;
        int attempt = 0;
        while (profileRepository.existsByUsername(candidate)) {
            attempt++;
            int suffix = (int) (Math.random() * 9000) + 1000; // 4-digit random
            candidate = base + suffix;
            if (attempt > 10) {
                candidate = base + System.currentTimeMillis(); // guaranteed unique
                break;
            }
        }

        return candidate;
    }
}
