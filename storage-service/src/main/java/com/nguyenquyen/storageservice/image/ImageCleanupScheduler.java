package com.nguyenquyen.storageservice.image;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "image-storage.cleanup.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ImageCleanupScheduler {

    private final ImageRepository imageRepository;
    private final MinioClient minioClient;

    @Value("${image-storage.cleanup.pending-expiry-hours:2}")
    private int pendingExpiryHours;

    @Scheduled(fixedDelayString = "${image-storage.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredPendingUploads() {
        Instant threshold = Instant.now().minus(pendingExpiryHours, ChronoUnit.HOURS);

        List<Image> expiredRecords = imageRepository
                .findExpiredPendingUploads(Image.ImageStatus.PENDING, threshold);

        if (expiredRecords.isEmpty()) {
            log.debug("No expired pending uploads found");
            return;
        }

        log.info("Found {} expired pending uploads to cleanup", expiredRecords.size());

        int cleaned = 0;
        for (Image record : expiredRecords) {
            if (processExpiredRecord(record)) {
                cleaned++;
            }
        }

        log.info("Cleanup completed. Processed: {}/{}", cleaned, expiredRecords.size());
    }

    private boolean processExpiredRecord(Image record) {
        try {
            tryRemoveFromMinio(record.getBucketName(), record.getObjectName());

            if (record.getThumbnailImageId() != null) {
                imageRepository.findById(record.getThumbnailImageId())
                        .ifPresent(thumb -> {
                            tryRemoveFromMinio(thumb.getBucketName(), thumb.getObjectName());
                            thumb.setStatus(Image.ImageStatus.EXPIRED);
                            imageRepository.save(thumb);
                        });
            }

            record.setStatus(Image.ImageStatus.EXPIRED);
            imageRepository.save(record);

            log.debug("Expired record cleaned: {}", record.getId());
            return true;

        } catch (Exception e) {
            log.error("Failed to cleanup expired record: {}", record.getId(), e);
            return false;
        }
    }

    private void tryRemoveFromMinio(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.debug("Object not found or already removed: {}/{}", bucket, objectName);
        }
    }
}