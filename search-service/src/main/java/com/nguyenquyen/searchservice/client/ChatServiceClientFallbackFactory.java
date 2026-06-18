package com.nguyenquyen.searchservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ChatServiceClientFallbackFactory implements FallbackFactory<ChatServiceClient> {

    @Override
    public ChatServiceClient create(Throwable cause) {
        return new ChatServiceClient() {
            @Override
            public void getChatById(UUID chatId) {
                log.error("Failed to get chat {} from chat-service", chatId, cause);
            }

            @Override
            public List<ChatResponse> getMyChats() {
                log.error("Failed to get my chats from chat-service, returning empty list", cause);
                return Collections.emptyList();
            }
        };
    }
}
