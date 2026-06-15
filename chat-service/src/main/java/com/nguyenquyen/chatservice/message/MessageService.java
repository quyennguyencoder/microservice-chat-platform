package com.nguyenquyen.chatservice.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MessageService {

    MessageResponse sendMessage(UUID chatId, String content, MessageType type, String imageId);

    MessageResponse sendMessage(UUID chatId, String senderId, String content, MessageType type, String imageId);

    Page<MessageResponse> getChatMessages(UUID chatId, Pageable pageable);

    List<MessageResponse> getAllChatMessages(UUID chatId);

    int markAsRead(UUID chatId);

    int markAsRead(UUID chatId, String userId);

    int getUnreadCount(UUID chatId);

    int getUnreadCount(UUID chatId, String userId);
    
    int getMessagePage(UUID chatId, UUID messageId, int size);
}