package com.nguyenquyen.conversationservice.repository;

import com.nguyenquyen.conversationservice.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;


@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {
    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);
}
