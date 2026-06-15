package com.nguyenquyen.chatservice.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.nguyenquyen.chatservice.chat.Chat;
import com.nguyenquyen.chatservice.chat.ChatService;
import com.nguyenquyen.chatservice.message.MessageResponse;
import com.nguyenquyen.chatservice.message.MessageService;
import com.nguyenquyen.chatservice.message.MessageType;
import com.nguyenquyen.chatservice.presence.PresenceService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChatService chatService;
    private final PresenceService presenceService;
    private final WebSocketSessionManager sessionManager;

    @MessageMapping("/chat")
    public void handleMessage(@Payload WsIncomingMessage incoming, Principal principal) {
        if (principal == null) {
            log.warn("Received message without principal");
            return;
        }

        String userId = principal.getName();
        UUID chatId = incoming.getChatId();

        log.debug("Received WS message: type={}, chatId={}, userId={}",
                incoming.getType(), chatId, userId);

        try {
            switch (incoming.getType()) {
                case SEND_MESSAGE -> handleSendMessage(userId, incoming);
                case TYPING_START -> handleTypingStart(userId, chatId);
                case TYPING_STOP -> handleTypingStop(userId, chatId);
                case MARK_READ -> handleMarkRead(userId, chatId);
                case JOIN_CHAT -> handleJoinChat(userId, chatId);
                case LEAVE_CHAT -> handleLeaveChat(userId, chatId);
                case HEARTBEAT -> handleHeartbeat(userId);
                default -> log.warn("Unknown message type: {}", incoming.getType());
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message: type={}, userId={}, error={}",
                    incoming.getType(), userId, e.getMessage(), e);
            sendToUser(userId, WsOutgoingMessage.error(e.getMessage()));
        }
    }

    private void handleSendMessage(String userId, WsIncomingMessage incoming) {
        UUID chatId = incoming.getChatId();
        log.info("Processing SEND_MESSAGE: userId={}, chatId={}", userId, chatId);

        MessageType type = incoming.getMessageType() != null
                ? incoming.getMessageType()
                : MessageType.TEXT;

        MessageResponse message = messageService.sendMessage(
                chatId,
                userId,
                incoming.getContent(),
                type,
                incoming.getImageId()
        );

        log.info("Message saved: id={}, chatId={}, from={}",
                message.id(), chatId, userId);

        WsOutgoingMessage outgoing = WsOutgoingMessage.newMessage(message);

        // Send to sender
        sendToUser(userId, outgoing);

        Chat chat = chatService.getChatEntityById(chatId);
        List<String> recipients = chat.getMemberIds().stream()
                .filter(id -> !id.equals(userId))
                .toList();

        for (String recipientId : recipients) {
            if (sessionManager.isUserOnline(recipientId)) {
                sendToUser(recipientId, outgoing);
                log.info("Message sent to recipient {} via WebSocket", recipientId);
            } else {
                log.info("Recipient {} is offline, message will be delivered via Kafka by MessageService", recipientId);
            }
        }
    }

    private void handleTypingStart(String userId, UUID chatId) {
        if (chatId == null) return;

        presenceService.startTyping(userId, chatId);

        Chat chat = chatService.getChatEntityById(chatId);
        WsOutgoingMessage typingMessage = WsOutgoingMessage.typing(chatId, userId);

        chat.getMemberIds().stream()
                .filter(id -> !id.equals(userId))
                .filter(sessionManager::isUserOnline)
                .forEach(recipientId -> sendToUser(recipientId, typingMessage));
    }

    private void handleTypingStop(String userId, UUID chatId) {
        if (chatId == null) return;

        presenceService.stopTyping(userId, chatId);

        Chat chat = chatService.getChatEntityById(chatId);
        WsOutgoingMessage stopTypingMessage = WsOutgoingMessage.stoppedTyping(chatId, userId);

        chat.getMemberIds().stream()
                .filter(id -> !id.equals(userId))
                .filter(sessionManager::isUserOnline)
                .forEach(recipientId -> sendToUser(recipientId, stopTypingMessage));
    }

    private void handleMarkRead(String userId, UUID chatId) {
        if (chatId == null) {
            log.warn("MARK_READ received without chatId");
            return;
        }

        log.debug("Processing MARK_READ: userId={}, chatId={}", userId, chatId);

        int count = messageService.markAsRead(chatId, userId);

        if (count > 0) {
            Chat chat = chatService.getChatEntityById(chatId);
            WsOutgoingMessage readMessage = WsOutgoingMessage.messagesRead(chatId, userId);

            chat.getMemberIds().stream()
                .filter(id -> !id.equals(userId))
                .filter(sessionManager::isUserOnline)
                .forEach(recipientId -> {
                    sendToUser(recipientId, readMessage);
                    log.debug("Sent MESSAGES_READ notification to {}", recipientId);
                });
        }
    }

    private void handleJoinChat(String userId, UUID chatId) {
        if (chatId == null) {
            log.warn("JOIN_CHAT received without chatId");
            return;
        }

        sessionManager.joinChat(userId, chatId);
        presenceService.setActiveChat(userId, chatId);

        log.debug("User {} joined chat {}", userId, chatId);

        handleMarkRead(userId, chatId);
    }

    private void handleLeaveChat(String userId, UUID chatId) {
        if (chatId == null) {
            log.warn("LEAVE_CHAT received without chatId");
            return;
        }

        sessionManager.leaveChat(userId, chatId);
        presenceService.removeActiveChat(userId);
        presenceService.stopTyping(userId, chatId);

        log.debug("User {} left chat {}", userId, chatId);
    }

    private void handleHeartbeat(String userId) {
        presenceService.updatePresence(userId);
        log.trace("Heartbeat received from user {}", userId);
    }

    private void sendToUser(String userId, WsOutgoingMessage message) {
        log.debug("Sending to user {}: {}", userId, message.getType());
        messagingTemplate.convertAndSend("/queue/chat/" + userId, message);
    }
}
