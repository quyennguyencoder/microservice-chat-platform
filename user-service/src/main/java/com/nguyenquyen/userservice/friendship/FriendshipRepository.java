package com.nguyenquyen.userservice.friendship;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    /**
     * Find a friendship between two users in either direction.
     */
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :userId1 AND f.addressee.id = :userId2)
           OR (f.requester.id = :userId2 AND f.addressee.id = :userId1)
    """)
    Optional<Friendship> findBetweenUsers(@Param("userId1") String userId1,
                                          @Param("userId2") String userId2);

    /**
     * Find a specific directional friendship (requester → addressee).
     */
    Optional<Friendship> findByRequesterIdAndAddresseeId(String requesterId, String addresseeId);

    /**
     * Get all accepted friends for a user (user can be either requester or addressee).
     */
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
          AND f.status = 'ACCEPTED'
    """)
    List<Friendship> findAcceptedFriendships(@Param("userId") String userId);

    /**
     * Get incoming friend requests (where the user is the addressee and status is PENDING).
     */
    List<Friendship> findByAddresseeIdAndStatus(String addresseeId, FriendshipStatus status);

    /**
     * Get outgoing friend requests (where the user is the requester and status is PENDING).
     */
    List<Friendship> findByRequesterIdAndStatus(String requesterId, FriendshipStatus status);

    /**
     * Check if a friendship exists between two users.
     */
    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE (f.requester.id = :userId1 AND f.addressee.id = :userId2)
           OR (f.requester.id = :userId2 AND f.addressee.id = :userId1)
    """)
    boolean existsBetweenUsers(@Param("userId1") String userId1,
                               @Param("userId2") String userId2);

    /**
     * Get blocked users by this user.
     */
    @Query("""
        SELECT f FROM Friendship f
        WHERE f.requester.id = :userId
          AND f.status = 'BLOCKED'
    """)
    List<Friendship> findBlockedByUser(@Param("userId") String userId);
}
