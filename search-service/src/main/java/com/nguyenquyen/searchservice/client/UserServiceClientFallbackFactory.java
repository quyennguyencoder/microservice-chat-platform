package com.nguyenquyen.searchservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public List<FriendshipResponse> getFriends() {
                log.error("Failed to fetch friends from user-service, returning empty list", cause);
                return Collections.emptyList();
            }
        };
    }
}
