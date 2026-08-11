package com.nguyenquyen.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "storage-service",
        url = "${services.storage-service.url}",
        fallbackFactory = StorageClientFallbackFactory.class
)
public interface StorageClient {

    @DeleteMapping("/api/v1/attachments/{attachmentId}")
    void deleteAttachment(@PathVariable String attachmentId);
}