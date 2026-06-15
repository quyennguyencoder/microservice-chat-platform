package com.nguyenquyen.userservice.friendship;

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
    public ResponseEntity<FriendshipResponse> sendFriendRequest(@PathVariable String userId) {
        return ResponseEntity.ok(friendshipService.sendFriendRequest(userId));
    }

    @PostMapping("/accept/{userId}")
    public ResponseEntity<FriendshipResponse> acceptFriendRequest(@PathVariable String userId) {
        return ResponseEntity.ok(friendshipService.acceptFriendRequest(userId));
    }

    @PostMapping("/decline/{userId}")
    public ResponseEntity<Void> declineFriendRequest(@PathVariable String userId) {
        friendshipService.declineFriendRequest(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeFriend(@PathVariable String userId) {
        friendshipService.removeFriend(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{userId}")
    public ResponseEntity<Void> blockUser(@PathVariable String userId) {
        friendshipService.blockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/block/{userId}")
    public ResponseEntity<Void> unblockUser(@PathVariable String userId) {
        friendshipService.unblockUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendshipResponse>> getFriends() {
        return ResponseEntity.ok(friendshipService.getFriends());
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<FriendshipResponse>> getIncomingRequests() {
        return ResponseEntity.ok(friendshipService.getIncomingRequests());
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<FriendshipResponse>> getOutgoingRequests() {
        return ResponseEntity.ok(friendshipService.getOutgoingRequests());
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<FriendshipCheckResponse> checkFriendship(@PathVariable String userId) {
        return ResponseEntity.ok(friendshipService.checkFriendship(userId));
    }
}
