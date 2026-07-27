package com.nguyenquyen.userservice.user.controller;

import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.common.util.SecurityUtils;
import com.nguyenquyen.userservice.user.dto.UserResponse;
import com.nguyenquyen.userservice.user.dto.UserUpdateRequest;
import com.nguyenquyen.userservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(SecurityUtils.requireCurrentUserId())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId)));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUsername(username)));
    }

    @PatchMapping(value = "/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {

        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(SecurityUtils.requireCurrentUserId(), userUpdateRequest)));
    }

}
