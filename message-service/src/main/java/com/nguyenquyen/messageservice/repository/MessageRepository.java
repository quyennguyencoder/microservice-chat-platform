package com.nguyenquyen.messageservice.repository;

import com.nguyenquyen.messageservice.entity.Message;
import com.nguyenquyen.messageservice.entity.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByChatIdOrderByCreatedAtDesc(UUID chatId, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Message m
        SET m.status = :readStatus
        WHERE m.chatId = :chatId
          AND m.senderId != :readerId
          AND m.status != :readStatus
        """)
    void markAllAsReadByChatId(
            @Param("chatId")     UUID chatId,
            @Param("readerId")   UUID readerId,
            @Param("readStatus") MessageStatus readStatus);


    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.chatId = :chatId
          AND m.senderId != :userId
          AND m.status != com.nguyenquyen.messageservice.entity.MessageStatus.READ
        """)
    long countUnreadByChatId(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId);
}
