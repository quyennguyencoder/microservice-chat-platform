package com.nguyenquyen.searchservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "user-service", contextId = "userServiceClient")
public interface UserServiceClient {

    @GetMapping("/api/v1/friends")
    List<FriendshipResponse> getFriends();
}
