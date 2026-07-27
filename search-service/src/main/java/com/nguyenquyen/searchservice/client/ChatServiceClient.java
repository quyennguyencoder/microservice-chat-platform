package com.nguyenquyen.searchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.nguyenquyen.searchservice.config.FeignConfig;

import java.util.List;
import java.util.UUID;
import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.common.constant.RedisConstants;

@FeignClient(name = "chat-service", configuration = FeignConfig.class, fallbackFactory = ChatServiceClientFallbackFactory.class)
public interface ChatServiceClient {

    @GetMapping("/api/v1/chats/{chatId}")
    ApiResponse<ChatResponse> getChatById(@PathVariable("chatId") UUID chatId);

    @org.springframework.cache.annotation.Cacheable(value = RedisConstants.MY_CHATS_CACHE, key = "T(com.nguyenquyen.common.util.SecurityUtils).requireCurrentUserId()")
    @GetMapping("/api/v1/chats")
    ApiResponse<List<ChatResponse>> getMyChats();
}
