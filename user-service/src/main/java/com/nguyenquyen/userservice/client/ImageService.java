package com.nguyenquyen.userservice.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageService {

    private final StorageClient storageClient;

    public void deleteImageSafely(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return;
        }

        try {
            storageClient.deleteImage(imageId);
            log.info("Deleted image: {}", imageId);
        } catch (FeignException.NotFound e) {
            log.warn("Image not found: {}", imageId);
        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageId, e);
        }
    }
}