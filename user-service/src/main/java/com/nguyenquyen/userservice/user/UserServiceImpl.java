package com.nguyenquyen.userservice.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nguyenquyen.userservice.client.ImageService;
import com.nguyenquyen.userservice.exception.UsernameAlreadyExistsException;
import com.nguyenquyen.userservice.exception.UserNotFoundException;
import com.nguyenquyen.userservice.kafka.UserEvent;
import com.nguyenquyen.userservice.kafka.UserEventPublisher;
import com.nguyenquyen.userservice.kafka.UserEventType;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ImageService imageService;
    private final UserEventPublisher eventPublisher;

    @Override
    public UserResponse getUserById(String userId) {
        return userMapper.toResponse(getUserByIdOrThrow(userId));
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = getUserByIdOrThrow(id);

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new UsernameAlreadyExistsException("Username taken: " + request.username());
            }
        }

        handleImageUpdate(request.imageId(), user.getImageId(), user::setImageId);
        handleImageUpdate(request.bannerImageId(), user.getBannerImageId(), user::setBannerImageId);

        userMapper.updateFromRequest(request, user);

        User saved = userRepository.save(user);

        eventPublisher.publish(UserEvent.builder()
                .type(UserEventType.USER_UPDATED.name())
                .userId(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .displayName(saved.getDisplayName())
                .description(saved.getDescription())
                .imageId(saved.getImageId())
                .bannerImageId(saved.getBannerImageId())
                .build());

        return userMapper.toResponse(saved);
    }

    private void handleImageUpdate(String newId, String currentId, Consumer<String> setter) {
        if (newId == null) {
            return;
        }

        if (newId.isBlank()) {
            imageService.deleteImageSafely(currentId);
            setter.accept(null);
            return;
        }

        if (!newId.equals(currentId)) {
            imageService.deleteImageSafely(currentId);
            setter.accept(newId);
        }
    }

    @Override
    public User getUserByIdOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    @Override
    public void validateUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found: " + userId);
        }
    }
}
