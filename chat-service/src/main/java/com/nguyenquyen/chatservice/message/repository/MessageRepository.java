package com.nguyenquyen.chatservice.message.repository;

import com.nguyenquyen.chatservice.message.entity.Message;
import com.nguyenquyen.chatservice.message.enums.MessageState;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt DESC")
    Page<Message> findByChatId(@Param("chatId") UUID chatId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt ASC")
    List<Message> findAllByChatIdOrdered(@Param("chatId") UUID chatId);

    Optional<Message> findFirstByChatIdOrderByCreatedAtDesc(UUID chatId);

    @Query("""
        SELECT m FROM Message m 
        WHERE m.id IN (
            SELECT m2.id FROM Message m2 
            WHERE m2.chat.id IN :chatIds 
            AND m2.createdAt = (
                SELECT MAX(m3.createdAt) FROM Message m3 WHERE m3.chat.id = m2.chat.id
            )
        )
    """)
    List<Message> findLatestMessagesForChats(@Param("chatIds") List<UUID> chatIds);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chat.id = :chatId AND m.createdAt > (SELECT m2.createdAt FROM Message m2 WHERE m2.id = :messageId)")
    long countNewerMessages(@Param("chatId") UUID chatId, @Param("messageId") UUID messageId);

    @Modifying
    @Query("""
        UPDATE Message m 
        SET m.state = :newState 
        WHERE m.chat.id = :chatId 
        AND m.senderId != :userId 
        AND m.state = :currentState
    """)
    int markMessagesAsRead(
            @Param("chatId") UUID chatId,
            @Param("userId") String userId,
            @Param("currentState") MessageState currentState,
            @Param("newState") MessageState newState
    );

    @Query("""
        SELECT COUNT(m) FROM Message m 
        WHERE m.chat.id = :chatId 
        AND m.senderId != :userId 
        AND m.state = :state
    """)
    int countUnreadMessages(
            @Param("chatId") UUID chatId,
            @Param("userId") String userId,
            @Param("state") MessageState state
    );

    @Query("SELECT m.imageId FROM Message m WHERE m.chat.id = :chatId AND m.imageId IS NOT NULL")
    List<String> findImageIdsByChatId(@Param("chatId") UUID chatId);

    interface ChatUnreadCount {
        UUID getChatId();
        int getUnreadCount();
    }

    @Query("""
        SELECT m.chat.id as chatId, COUNT(m) as unreadCount 
        FROM Message m 
        WHERE m.chat.id IN :chatIds 
        AND m.senderId != :userId 
        AND m.state = :state 
        GROUP BY m.chat.id
    """)
    List<ChatUnreadCount> countUnreadMessagesForChats(
            @Param("chatIds") List<UUID> chatIds,
            @Param("userId") String userId,
            @Param("state") MessageState state
    );
}