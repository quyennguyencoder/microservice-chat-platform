package com.nguyenquyen.userservice.friendship.controller;

import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.userservice.friendship.dto.FriendshipCheckResponse;
import com.nguyenquyen.userservice.friendship.dto.FriendshipResponse;
import com.nguyenquyen.userservice.friendship.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request/{userId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> sendFriendRequest(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.sendFriendRequest(userId)));
    }

    @PostMapping("/accept/{userId}")
    public ResponseEntity<ApiResponse<FriendshipResponse>> acceptFriendRequest(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.acceptFriendRequest(userId)));
    }

    @PostMapping("/decline/{userId}")
    public ResponseEntity<ApiResponse<Void>> declineFriendRequest(@PathVariable String userId) {
        friendshipService.declineFriendRequest(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(@PathVariable String userId) {
        friendshipService.removeFriend(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable String userId) {
        friendshipService.blockUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable String userId) {
        friendshipService.unblockUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getFriends() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getFriends()));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getIncomingRequests() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getIncomingRequests()));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<ApiResponse<List<FriendshipResponse>>> getOutgoingRequests() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getOutgoingRequests()));
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<ApiResponse<FriendshipCheckResponse>> checkFriendship(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.checkFriendship(userId)));
    }
}
