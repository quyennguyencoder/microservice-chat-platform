package com.nguyenquyen.conversationservice.repository;


import com.nguyenquyen.conversationservice.entity.Chat;
import com.nguyenquyen.conversationservice.entity.ChatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    /**
     * Find a private chat that has exactly these two participants.
     * Used to prevent duplicate private chats between the same pair of users.
     *
     * @param userA    first user UUID
     * @param userB    second user UUID
     * @param type     chat type (PRIVATE)
     * @return optional existing private chat between these two users
     */
    @Query("""
        SELECT c FROM Chat c
        JOIN c.participants p1 ON p1.userId = :userA
        JOIN c.participants p2 ON p2.userId = :userB
        WHERE c.type = :type
        """)
    Optional<Chat> findPrivateChatBetweenUsers(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB,
            @Param("type") ChatType type);

    /**
     * Find all chats where the given user is a participant.
     * Results are sorted by updatedAt DESC (most recently active chats first).
     *
     * @param userId   authenticated user UUID
     * @param pageable pagination parameters
     * @return paginated page of the user's chats
     */
    @Query("""
        SELECT c FROM Chat c
        JOIN c.participants p ON p.userId = :userId
        ORDER BY c.updatedAt DESC
        """)
    Page<Chat> findAllByParticipantUserId(
            @Param("userId") UUID userId,
            Pageable pageable);

    /**
     * Find a GROUP chat by its associated group ID.
     * Used for idempotency on group creation and for member sync operations.
     *
     * @param groupId the group UUID from group-service
     * @return optional chat linked to this group
     */
    Optional<Chat> findByGroupId(UUID groupId);
}
