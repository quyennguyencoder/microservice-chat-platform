package com.nguyenquyen.userservice.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.nguyenquyen.common.util.SecurityUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getUserById(SecurityUtils.requireCurrentUserId()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PatchMapping(value = "/me")
    public ResponseEntity<UserResponse> updateUser(
            @Valid @RequestBody UserUpdateRequest userUpdateRequest) {

        return ResponseEntity.ok(userService.updateUser(SecurityUtils.requireCurrentUserId(), userUpdateRequest));
    }

}