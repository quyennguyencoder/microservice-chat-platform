package com.nguyenquyen.userservice.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nguyenquyen.userservice.kafka.UserEvent;
import com.nguyenquyen.userservice.kafka.UserEventPublisher;
import com.nguyenquyen.userservice.kafka.UserEventType;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSynchronizer {

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    @Transactional
    public void synchronizeUser(Jwt jwt) {
        String id = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        userRepository.findById(id).ifPresentOrElse(
                user -> {
                    if (!Objects.equals(user.getEmail(), email)) {
                        log.info("Updating email for user {}: {} -> {}",
                                id, user.getEmail(), email);
                        user.setEmail(email);
                        user.setUpdatedAt(Instant.now());
                        userRepository.save(user);

                        publishUserEvent(user, UserEventType.USER_UPDATED);
                    }
                },
                () -> {
                    User newUser = createNewUser(jwt, id, email);
                    publishUserEvent(newUser, UserEventType.USER_CREATED);
                }
        );
    }

    private User createNewUser(Jwt jwt, String id, String email) {
        String username = extractUsername(jwt, email);
        String displayName = extractDisplayName(jwt, username);

        log.info("Creating new user from token: id={}, email={}, username={}",
                id, email, username);

        User newUser = User.builder()
                .id(id)
                .email(email)
                .username(username)
                .displayName(displayName)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return userRepository.save(newUser);
    }

    private String extractUsername(Jwt jwt, String email) {
        String username = jwt.getClaimAsString("preferred_username");

        if (username == null || username.isBlank()) {
            username = jwt.getClaimAsString("username");
        }

        if (username == null || username.isBlank()) {
            if (email != null && email.contains("@")) {
                username = email.split("@")[0] + "_" +
                        (System.currentTimeMillis() % 100000);
            } else {
                username = "user_" + java.util.UUID.randomUUID()
                        .toString().substring(0, 8);
            }
            log.warn("Username not found in token, generated: {}", username);
        }

        return username;
    }

    private String extractDisplayName(Jwt jwt, String fallbackUsername) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        if (givenName != null || familyName != null) {
            return ((givenName != null ? givenName : "") + " " +
                    (familyName != null ? familyName : "")).trim();
        }

        return fallbackUsername;
    }

    private void publishUserEvent(User user, UserEventType eventType) {
        eventPublisher.publish(UserEvent.builder()
                .type(eventType.name())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .description(user.getDescription())
                .imageId(user.getImageId())
                .bannerImageId(user.getBannerImageId())
                .build());
    }
}
