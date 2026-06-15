package com.nguyenquyen.chatservice.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    /**
     * Find all chats where a user is a member, ordered by latest message.
     */
    @Query("""
        SELECT c FROM Chat c
        JOIN c.members m
        LEFT JOIN c.messages msg
        WHERE m.userId = :userId
        GROUP BY c
        ORDER BY MAX(msg.createdAt) DESC NULLS LAST, c.updatedAt DESC
    """)
    List<Chat> findAllByUserIdOrderedByLastMessage(@Param("userId") String userId);

    /**
     * Find a PRIVATE chat between exactly two users.
     */
    @Query("""
        SELECT c FROM Chat c
        WHERE c.type = 'PRIVATE'
          AND EXISTS (SELECT 1 FROM ChatMember m1 WHERE m1.chat = c AND m1.userId = :userId1)
          AND EXISTS (SELECT 1 FROM ChatMember m2 WHERE m2.chat = c AND m2.userId = :userId2)
    """)
    Optional<Chat> findPrivateChatBetween(@Param("userId1") String userId1,
                                          @Param("userId2") String userId2);
}