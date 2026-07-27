package com.nguyenquyen.searchservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.nguyenquyen.common.dto.response.ApiResponse;

@Slf4j
@Component
public class ChatServiceClientFallbackFactory implements FallbackFactory<ChatServiceClient> {

    @Override
    public ChatServiceClient create(Throwable cause) {
        return new ChatServiceClient() {
            @Override
            public ApiResponse<ChatResponse> getChatById(UUID chatId) {
                log.error("Failed to get chat {} from chat-service", chatId, cause);
                return ApiResponse.success(null);
            }

            @Override
            public ApiResponse<List<ChatResponse>> getMyChats() {
                log.error("Failed to get my chats from chat-service, returning empty list", cause);
                return ApiResponse.success(Collections.emptyList());
            }
        };
    }
}
