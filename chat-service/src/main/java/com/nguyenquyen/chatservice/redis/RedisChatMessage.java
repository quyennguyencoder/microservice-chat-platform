package com.nguyenquyen.chatservice.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nguyenquyen.chatservice.websocket.WsOutgoingMessage;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisChatMessage implements Serializable {
    private String recipientId; // Who should receive this message
    private UUID chatId; // The chat room ID (for broadcast to all members)
    private WsOutgoingMessage message;
    private String sourceServerId; // Optional: To avoid processing own messages if needed
}
