package com.nguyenquyen.chatservice.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMemberRepository extends JpaRepository<ChatMember, UUID> {

    List<ChatMember> findByChatId(UUID chatId);

    Optional<ChatMember> findByChatIdAndUserId(UUID chatId, String userId);

    boolean existsByChatIdAndUserId(UUID chatId, String userId);

    void deleteByChatIdAndUserId(UUID chatId, String userId);

    @Query("SELECT cm.chat.id FROM ChatMember cm WHERE cm.userId = :userId")
    List<UUID> findChatIdsByUserId(@Param("userId") String userId);
}
