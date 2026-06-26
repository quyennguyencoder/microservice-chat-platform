package com.nguyenquyen.searchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.nguyenquyen.searchservice.config.FeignConfig;

import java.util.List;

@FeignClient(name = "user-service", contextId = "userServiceClient", configuration = FeignConfig.class, fallbackFactory = UserServiceClientFallbackFactory.class)
public interface UserServiceClient {

    @GetMapping("/api/v1/friends")
    List<FriendshipResponse> getFriends();
}
