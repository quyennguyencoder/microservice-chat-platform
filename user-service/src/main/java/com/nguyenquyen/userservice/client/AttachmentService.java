package com.nguyenquyen.userservice.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentService {

    private final StorageClient storageClient;

    public void deleteAttachmentSafely(String attachmentId) {
        if (attachmentId == null || attachmentId.isBlank()) {
            return;
        }

        try {
            storageClient.deleteAttachment(attachmentId);
            log.info("Deleted attachment: {}", attachmentId);
        } catch (FeignException.NotFound e) {
            log.warn("Attachment not found: {}", attachmentId);
        } catch (Exception e) {
            log.error("Failed to delete attachment: {}", attachmentId, e);
        }
    }
}