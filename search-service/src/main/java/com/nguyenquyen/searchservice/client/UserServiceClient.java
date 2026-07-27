package com.nguyenquyen.searchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.nguyenquyen.searchservice.config.FeignConfig;
import com.nguyenquyen.common.dto.response.ApiResponse;
import com.nguyenquyen.common.constant.RedisConstants;

import java.util.List;

@FeignClient(name = "user-service", contextId = "userServiceClient", configuration = FeignConfig.class, fallbackFactory = UserServiceClientFallbackFactory.class)
public interface UserServiceClient {

    @org.springframework.cache.annotation.Cacheable(value = RedisConstants.MY_FRIENDS_CACHE, key = "T(com.nguyenquyen.common.util.SecurityUtils).requireCurrentUserId()")
    @GetMapping("/api/v1/friends")
    ApiResponse<List<FriendshipResponse>> getFriends();
}
