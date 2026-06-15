package com.nguyenquyen.storageservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initBuckets() {
        try {
            MinioClient client = minioClient();

            createBucketIfNotExists(client, minioProperties.getBucketName());

            if (minioProperties.getThumbnailBucket() != null
                    && !minioProperties.getThumbnailBucket().isEmpty()) {
                createBucketIfNotExists(client, minioProperties.getThumbnailBucket());
            }

            log.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing MinIO buckets", e);
        }
    }

    private void createBucketIfNotExists(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );

        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket '{}' created", bucket);
        }
    }
}