package com.nguyenquyen.searchservice.message;

import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class MessageSearchMapper {

    public MessageSearchResult toSearchResult(MessageDocument document) {
        if (document == null) {
            return null;
        }

        return MessageSearchResult.builder()
                .messageId(document.getMessageId())
                .chatId(document.getChatId())
                .senderId(document.getSenderId())
                .content(document.getContent())
                .type(document.getType())
                .createdAt(document.getCreatedAt() != null ? Instant.ofEpochMilli(document.getCreatedAt()) : null)
                .updatedAt(document.getUpdatedAt() != null ? Instant.ofEpochMilli(document.getUpdatedAt()) : null)
                .build();
    }
}
