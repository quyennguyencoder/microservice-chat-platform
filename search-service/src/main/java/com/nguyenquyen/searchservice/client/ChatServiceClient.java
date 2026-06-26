package com.nguyenquyen.searchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.nguyenquyen.searchservice.config.FeignConfig;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "chat-service", configuration = FeignConfig.class, fallbackFactory = ChatServiceClientFallbackFactory.class)
public interface ChatServiceClient {

    @GetMapping("/api/v1/chats/{chatId}")
    void getChatById(@PathVariable("chatId") UUID chatId);

    @GetMapping("/api/v1/chats")
    List<ChatResponse> getMyChats();
}
