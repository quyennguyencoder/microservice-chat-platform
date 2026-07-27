package com.nguyenquyen.userservice.user.service;

import com.nguyenquyen.userservice.user.dto.UserResponse;
import com.nguyenquyen.userservice.user.dto.UserUpdateRequest;
import com.nguyenquyen.userservice.user.entity.User;

public interface UserService {

    UserResponse getUserById(String userId);

    UserResponse getUserByUsername(String username);

    UserResponse updateUser(String id, UserUpdateRequest request);

    User getUserByIdOrThrow(String id);

    void validateUserExists(String userId);
}
