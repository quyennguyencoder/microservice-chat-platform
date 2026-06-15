package com.nguyenquyen.storageservice.image;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nguyenquyen.storageservice.exception.ImageStorageException;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUrlService {

    private final MinioClient minioClient;

    @Value("${image-storage.presigned-url-expiry-minutes:15}")
    private int presignedUploadExpiryMinutes;

    @Value("${image-storage.download-url-expiry-hours:24}")
    private int downloadUrlExpiryHours;

    public String generatePresignedPutUrl(String bucket, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(presignedUploadExpiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT URL", e);
            throw new ImageStorageException("Failed to generate upload URL", e);
        }
    }

    public String generatePresignedGetUrl(String bucket, String objectName, Integer expirySeconds) {
        try {
            int expiry = (expirySeconds != null && expirySeconds > 0)
                    ? expirySeconds
                    : downloadUrlExpiryHours * 3600;

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL", e);
            throw new ImageStorageException("Failed to generate download URL", e);
        }
    }

    public String generatePresignedGetUrlSafely(String bucket, String objectName, Integer expirySeconds) {
        try {
            return generatePresignedGetUrl(bucket, objectName, expirySeconds);
        } catch (Exception e) {
            log.warn("Failed to generate URL for object: {}/{}", bucket, objectName);
            return null;
        }
    }

    public long calculateExpiryTimestamp(int minutes) {
        return System.currentTimeMillis() + (minutes * 60 * 1000L);
    }

    public long calculateExpiryTimestampSeconds(int seconds) {
        return System.currentTimeMillis() + (seconds * 1000L);
    }
}
