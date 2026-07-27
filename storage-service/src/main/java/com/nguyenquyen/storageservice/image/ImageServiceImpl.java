package com.nguyenquyen.storageservice.image;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nguyenquyen.storageservice.config.MinioProperties;
import com.nguyenquyen.common.exception.AppException;
import com.nguyenquyen.common.exception.ErrorCode;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;
    private final ImageUrlService imageUrlService;

    @Value("${image-storage.max-file-size:20971520}")
    private long maxFileSize;

    @Value("${image-storage.allowed-content-types:image/jpeg,image/jpg,image/png,image/gif,image/webp}")
    private Set<String> allowedContentTypes;

    @Value("${image-storage.presigned-url-expiry-minutes:15}")
    private int presignedUploadExpiryMinutes;

    @Value("${image-storage.download-url-expiry-hours:24}")
    private int downloadUrlExpiryHours;

    @Override
    @Transactional
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        validateUploadRequest(request);

        String imageId = generateImageId();
        String extension = extractFileExtension(request.getFileName());
        String objectName = buildObjectName(imageId, request.getCategory(), extension);

        String uploadUrl = imageUrlService.generatePresignedPutUrl(
                minioProperties.getBucketName(), objectName
        );

        Image record = imageMapper.toEntity(request, imageId, objectName, minioProperties.getBucketName());

        imageRepository.save(record);

        long expiresAt = imageUrlService.calculateExpiryTimestamp(presignedUploadExpiryMinutes);
        Map<String, String> headers = Map.of("Content-Type", request.getContentType());

        log.info("Generated presigned upload URL. ImageId: {}, dimensions: {}x{}",
                imageId,
                request.getWidth(),
                request.getHeight());

        return imageMapper.toPresignedResponse(
                imageId,
                uploadUrl,
                expiresAt,
                headers,
                request.getWidth(),
                request.getHeight()
        );
    }

    @Override
    @Transactional
    public ConfirmUploadResponse confirmUpload(ConfirmUploadRequest request) {
        Image record = findImageById(request.getImageId());

        if (record.isConfirmed()) {
            log.warn("Image already confirmed: {}", request.getImageId());
            return buildConfirmResponse(record);
        }

        if (!record.isPending()) {
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, "Cannot confirm image with status: " + record.getStatus());
        }

        try {
            StatObjectResponse stat = verifyObjectExistsInMinio(record);

            String fileName = request.getFileName() != null ? request.getFileName() : record.getFileName();
            record.setFileSize(stat.size());
            record.setContentType(stat.contentType());
            record.setFileName(fileName);
            record.setStatus(Image.ImageStatus.CONFIRMED);
            record.setConfirmedAt(Instant.now());

            imageRepository.save(record);
            log.info("Upload confirmed. ImageId: {}", request.getImageId());

            return buildConfirmResponse(record);

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new AppException(ErrorCode.IMAGE_NOT_FOUND, "Image file not found in storage.");
            }
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, "Failed to verify upload", e);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to confirm upload: {}", request.getImageId(), e);
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, "Failed to confirm upload", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ImageUrlResponse getImageUrl(String imageId, Integer expirySeconds) {
        Image record = findConfirmedImage(imageId);

        int expiry = (expirySeconds != null && expirySeconds > 0)
                ? expirySeconds
                : downloadUrlExpiryHours * 3600;

        String url = imageUrlService.generatePresignedGetUrl(
                record.getBucketName(), record.getObjectName(), expiry
        );
        long expiresAt = imageUrlService.calculateExpiryTimestampSeconds(expiry);

        return imageMapper.toUrlResponse(imageId, url, expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public ImageMetadata getImageMetadata(String imageId) {
        Image record = findConfirmedImage(imageId);
        return imageMapper.toMetadata(record);
    }

    @Override
    @Transactional
    public void deleteImage(String imageId) {
        Image record = findImageById(imageId);

        try {
            deleteFromMinio(record.getBucketName(), record.getObjectName());

            record.setStatus(Image.ImageStatus.DELETED);
            imageRepository.save(record);

            log.info("Image deleted: {}", imageId);

        } catch (Exception e) {
            log.error("Failed to delete image: {}", imageId, e);
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, "Failed to delete image", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageMetadata> listImagesByCategory(String category) {
        List<Image> records = (category != null)
                ? imageRepository.findByCategoryAndStatus(category, Image.ImageStatus.CONFIRMED)
                : imageRepository.findByStatus(Image.ImageStatus.CONFIRMED);

        return imageMapper.toMetadataList(records);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadImage(String imageId) {
        Image record = findConfirmedImage(imageId);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download image: {}", imageId, e);
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, "Failed to download image", e);
        }
    }

    private void validateUploadRequest(PresignedUploadRequest request) {
        if (request.getFileSize() > maxFileSize) {
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, String.format(
                    "File size %d bytes exceeds maximum %d bytes",
                    request.getFileSize(), maxFileSize
            ));
        }

        String contentType = request.getContentType().toLowerCase();
        if (!allowedContentTypes.contains(contentType)) {
            throw new AppException(ErrorCode.IMAGE_STORAGE_ERROR, String.format(
                    "Content type '%s' is not allowed. Allowed: %s",
                    contentType, allowedContentTypes
            ));
        }
    }

    private ConfirmUploadResponse buildConfirmResponse(Image record) {
        String imageUrl = imageUrlService.generatePresignedGetUrlSafely(
                record.getBucketName(),
                record.getObjectName(),
                null
        );

        return imageMapper.toConfirmResponse(
                record,
                imageUrl
        );
    }

    private String generateImageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String buildObjectName(String imageId, String category, String extension) {
        StringBuilder sb = new StringBuilder();

        if (category != null && !category.isBlank()) {
            sb.append(category.toLowerCase()).append("/");
        }

        sb.append(LocalDate.now()).append("/");
        sb.append(imageId).append(".").append(extension);

        return sb.toString();
    }

    private Image findImageById(String imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND, "Image not found: " + imageId));
    }

    private Image findConfirmedImage(String imageId) {
        return imageRepository.findByIdAndStatus(imageId, Image.ImageStatus.CONFIRMED)
                .orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND, "Confirmed image not found: " + imageId));
    }

    private StatObjectResponse verifyObjectExistsInMinio(Image record) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(record.getBucketName())
                        .object(record.getObjectName())
                        .build()
        );
    }

    private void deleteFromMinio(String bucket, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
    }
}

