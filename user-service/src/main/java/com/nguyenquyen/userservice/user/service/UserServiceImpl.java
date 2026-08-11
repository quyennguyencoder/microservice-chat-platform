package com.nguyenquyen.userservice.user.service;

import com.nguyenquyen.common.exception.AppException;
import com.nguyenquyen.common.exception.ErrorCode;
import com.nguyenquyen.common.kafka.event.UserEvent;
import com.nguyenquyen.common.kafka.event.UserEventType;
import com.nguyenquyen.userservice.client.AttachmentService;
import com.nguyenquyen.userservice.kafka.UserEventPublisher;
import com.nguyenquyen.userservice.user.dto.UserResponse;
import com.nguyenquyen.userservice.user.dto.UserUpdateRequest;
import com.nguyenquyen.userservice.user.entity.User;
import com.nguyenquyen.userservice.user.mapper.UserMapper;
import com.nguyenquyen.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AttachmentService attachmentService;
    private final UserEventPublisher eventPublisher;

    @Override
    public UserResponse getUserById(String userId) {
        return userMapper.toResponse(getUserByIdOrThrow(userId));
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + username));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = getUserByIdOrThrow(id);

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS, "Username taken: " + request.username());
            }
        }

        handleImageUpdate(request.avatar(), user.getAvatar(), user::setAvatar);
        handleImageUpdate(request.banner(), user.getBanner(), user::setBanner);

        userMapper.updateFromRequest(request, user);

        User saved = userRepository.save(user);

        eventPublisher.publish(UserEvent.builder()
                .type(UserEventType.USER_UPDATED.name())
                .userId(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .displayName(saved.getDisplayName())
                .description(saved.getDescription())
                .avatar(saved.getAvatar())
                .banner(saved.getBanner())
                .build());

        return userMapper.toResponse(saved);
    }

    private void handleImageUpdate(String newId, String currentId, Consumer<String> setter) {
        if (newId == null) {
            return;
        }

        if (newId.isBlank()) {
            attachmentService.deleteAttachmentSafely(currentId);
            setter.accept(null);
            return;
        }

        if (!newId.equals(currentId)) {
            attachmentService.deleteAttachmentSafely(currentId);
            setter.accept(newId);
        }
    }

    @Override
    public User getUserByIdOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + id));
    }

    @Override
    public void validateUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId);
        }
    }
}
