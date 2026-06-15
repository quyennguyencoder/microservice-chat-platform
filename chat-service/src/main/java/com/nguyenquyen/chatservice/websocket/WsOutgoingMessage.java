package com.nguyenquyen.chatservice.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nguyenquyen.chatservice.message.MessageResponse;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsOutgoingMessage {

    private WsMessageType type;
    private UUID chatId;
    private String userId;
    private MessageResponse message;
    private String error;
    private Object payload; // Generic payload for group updates, member changes, etc.

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static WsOutgoingMessage newMessage(MessageResponse message) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.NEW_MESSAGE)
                .chatId(message.chatId())
                .message(message)
                .build();
    }

    public static WsOutgoingMessage typing(UUID chatId, String userId) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.USER_TYPING)
                .chatId(chatId)
                .userId(userId)
                .build();
    }

    public static WsOutgoingMessage stoppedTyping(UUID chatId, String userId) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.USER_STOPPED_TYPING)
                .chatId(chatId)
                .userId(userId)
                .build();
    }

    public static WsOutgoingMessage messagesRead(UUID chatId, String userId) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.MESSAGES_READ)
                .chatId(chatId)
                .userId(userId)
                .build();
    }

    public static WsOutgoingMessage userOnline(String userId) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.USER_ONLINE)
                .userId(userId)
                .build();
    }

    public static WsOutgoingMessage userOffline(String userId) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.USER_OFFLINE)
                .userId(userId)
                .build();
    }

    public static WsOutgoingMessage groupUpdated(UUID chatId, Object payload) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.GROUP_UPDATED)
                .chatId(chatId)
                .payload(payload)
                .build();
    }

    public static WsOutgoingMessage memberChange(UUID chatId, String userId, Object payload) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.MEMBER_CHANGE)
                .chatId(chatId)
                .userId(userId)
                .payload(payload)
                .build();
    }

    public static WsOutgoingMessage error(String errorMessage) {
        return WsOutgoingMessage.builder()
                .type(WsMessageType.ERROR)
                .error(errorMessage)
                .build();
    }
}
