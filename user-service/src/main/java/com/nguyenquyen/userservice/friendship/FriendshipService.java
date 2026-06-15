package com.nguyenquyen.userservice.friendship;

import java.util.List;

public interface FriendshipService {

    /**
     * Send a friend request to another user.
     */
    FriendshipResponse sendFriendRequest(String targetUserId);

    /**
     * Accept a pending friend request from another user.
     */
    FriendshipResponse acceptFriendRequest(String requesterUserId);

    /**
     * Decline a pending friend request from another user.
     */
    void declineFriendRequest(String requesterUserId);

    /**
     * Remove an existing friendship (unfriend).
     */
    void removeFriend(String targetUserId);

    /**
     * Block a user. Removes any existing friendship.
     */
    void blockUser(String targetUserId);

    /**
     * Unblock a previously blocked user.
     */
    void unblockUser(String targetUserId);

    /**
     * Get all accepted friends of the current user.
     */
    List<FriendshipResponse> getFriends();

    /**
     * Get incoming (received) friend requests.
     */
    List<FriendshipResponse> getIncomingRequests();

    /**
     * Get outgoing (sent) friend requests.
     */
    List<FriendshipResponse> getOutgoingRequests();

    /**
     * Check friendship status with another user.
     */
    FriendshipCheckResponse checkFriendship(String targetUserId);
}
