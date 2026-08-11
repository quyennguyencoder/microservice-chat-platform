package com.nguyenquyen.userservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StorageClientFallbackFactory implements FallbackFactory<StorageClient> {

    @Override
    public StorageClient create(Throwable cause) {
        return attachmentId -> log.error("Failed to delete attachment: {}", attachmentId, cause);
    }
}