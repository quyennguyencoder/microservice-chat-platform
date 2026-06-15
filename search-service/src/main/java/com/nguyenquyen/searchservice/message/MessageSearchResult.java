package com.nguyenquyen.searchservice.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchResult {
    private String messageId;
    private String chatId;
    private String senderId;
    private String content;
    private String type;
    private Instant createdAt;

    // Highlighted fragments
    private String highlightedContent;
}
