package com.nguyenquyen.storageservice.image;

import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

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

        // Fetch up to 1000 records at a time to prevent OOM
        List<Image> expiredRecords = imageRepository
                .findExpiredPendingUploads(Image.ImageStatus.PENDING, threshold, PageRequest.of(0, 1000));

        if (expiredRecords.isEmpty()) {
            log.debug("No expired pending uploads found");
            return;
        }

        log.info("Found {} expired pending uploads to cleanup in this batch", expiredRecords.size());

        String bucket = expiredRecords.get(0).getBucketName();

        List<DeleteObject> objects = expiredRecords.stream()
                .map(img -> new DeleteObject(img.getObjectName()))
                .collect(Collectors.toList());

        try {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder().bucket(bucket).objects(objects).build()
            );
            
            // Need to iterate results to actually execute the removal (lazy evaluated in MinIO SDK)
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                log.error("Failed to delete object {}: {}", error.objectName(), error.message());
            }
        } catch (Exception e) {
            log.error("Failed to bulk remove objects from bucket {}", bucket, e);
        }

        for (Image record : expiredRecords) {
            record.setStatus(Image.ImageStatus.EXPIRED);
        }

        imageRepository.saveAll(expiredRecords);
        log.info("Cleanup completed for batch of {}", expiredRecords.size());
    }
}