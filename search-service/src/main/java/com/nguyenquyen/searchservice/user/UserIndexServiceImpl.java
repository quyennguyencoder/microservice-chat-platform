package com.nguyenquyen.searchservice.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.nguyenquyen.searchservice.exception.IndexingException;
import com.nguyenquyen.searchservice.kafka.event.UserEvent;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserIndexServiceImpl implements UserIndexService {

    private final UserSearchRepository userSearchRepository;

    @Override
    public void indexUser(UserEvent event) {
        try {
            UserDocument document = mapToDocument(event);
            userSearchRepository.save(document);
            log.info("User indexed: id={}, username={}", document.getId(), document.getUsername());
        } catch (Exception e) {
            log.error("Failed to index user: {}", event.getUserId(), e);
            throw new IndexingException("Failed to index user: " + event.getUserId(), e);
        }
    }

    @Override
    public void updateUser(UserEvent event) {
        try {
            String userId = event.getUserId();

            Optional<UserDocument> existingOpt = userSearchRepository.findById(userId);

            if (existingOpt.isEmpty()) {
                log.warn("User not found for update, indexing as new: {}", userId);
                indexUser(event);
                return;
            }

            UserDocument existing = existingOpt.get();
            updateDocumentFromEvent(existing, event);
            existing.setUpdatedAt(Instant.now());

            userSearchRepository.save(existing);
            log.info("User updated: id={}", userId);

        } catch (Exception e) {
            log.error("Failed to update user: {}", event.getUserId(), e);
            throw new IndexingException("Failed to update user: " + event.getUserId(), e);
        }
    }



    @Override
    public void deleteUser(String userId) {
        try {
            if (userSearchRepository.existsById(userId)) {
                userSearchRepository.deleteById(userId);
                log.info("User deleted from index: {}", userId);
            } else {
                log.warn("User not found for deletion: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to delete user: {}", userId, e);
            throw new IndexingException("Failed to delete user: " + userId, e);
        }
    }

    @Override
    public Optional<UserDocument> findById(String userId) {
        return userSearchRepository.findById(userId);
    }

    @Override
    public long count() {
        return userSearchRepository.count();
    }

    private UserDocument mapToDocument(UserEvent event) {
        return UserDocument.builder()
                .id(event.getUserId())
                .username(event.getUsername())
                .email(event.getEmail())
                .description(event.getDescription())
                .imageId(event.getImageId())
                .bannerImageId(event.getBannerImageId())

                .createdAt(event.getTimestamp())
                .updatedAt(Instant.now())
                .build();
    }

    private void updateDocumentFromEvent(UserDocument document, UserEvent event) {
        if (event.getUsername() != null) {
            document.setUsername(event.getUsername());
        }
        if (event.getDescription() != null) {
            document.setDescription(event.getDescription());
        }
        if (event.getImageId() != null) {
            document.setImageId(event.getImageId());
        }
        if (event.getBannerImageId() != null) {
            document.setBannerImageId(event.getBannerImageId());
        }
    }
}
