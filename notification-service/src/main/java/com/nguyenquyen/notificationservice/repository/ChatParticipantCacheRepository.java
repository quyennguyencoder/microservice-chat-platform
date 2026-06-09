package com.nguyenquyen.notificationservice.repository;

import com.nguyenquyen.notificationservice.entity.ChatParticipantCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface ChatParticipantCacheRepository extends JpaRepository<ChatParticipantCache, UUID> {

    /**
     * Returns all participant UUIDs for a given chat.
     *
     * @param chatId the chat's UUID
     * @return list of participant cache entries
     */
    List<ChatParticipantCache> findByChatId(UUID chatId);

    /**
     * Checks if a (chatId, userId) pair already exists in the cache (idempotency guard).
     *
     * @param chatId the chat's UUID
     * @param userId the user's UUID
     * @return {@code true} if the entry exists
     */
    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);

    /**
     * Removes a specific participant from a chat in the cache.
     *
     * @param chatId the chat's UUID
     * @param userId the user's UUID
     */
    @Modifying
    @Query("DELETE FROM ChatParticipantCache c WHERE c.chatId = :chatId AND c.userId = :userId")
    void deleteByChatIdAndUserId(@Param("chatId") UUID chatId, @Param("userId") UUID userId);

    /**
     * Removes all participants for a chat (used when a group chat is deleted).
     *
     * @param chatId the chat's UUID
     */
    @Modifying
    @Query("DELETE FROM ChatParticipantCache c WHERE c.chatId = :chatId")
    void deleteAllByChatId(@Param("chatId") UUID chatId);
}
