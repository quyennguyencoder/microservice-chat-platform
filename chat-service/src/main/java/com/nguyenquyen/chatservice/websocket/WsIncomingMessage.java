package com.nguyenquyen.chatservice.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.nguyenquyen.chatservice.message.MessageType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsIncomingMessage {

    private WsMessageType type;
    private UUID chatId;
    private String content;
    private MessageType messageType;
    private String imageId;
}
