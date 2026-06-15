package com.nguyenquyen.chatservice.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt DESC")
    Page<Message> findByChatId(@Param("chatId") UUID chatId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.createdAt ASC")
    List<Message> findAllByChatIdOrdered(@Param("chatId") UUID chatId);

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
}