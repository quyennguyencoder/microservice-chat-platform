package com.nguyenquyen.storageservice.attachment;

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
public class AttachmentServiceImpl implements AttachmentService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentMapper attachmentMapper;
    private final AttachmentUrlService attachmentUrlService;

    @Value("${attachment-storage.max-file-size:20971520}")
    private long maxFileSize;

    @Value("${attachment-storage.allowed-content-types:image/jpeg,image/jpg,image/png,image/gif,image/webp,application/pdf}")
    private Set<String> allowedContentTypes;

    @Value("${attachment-storage.presigned-url-expiry-minutes:15}")
    private int presignedUploadExpiryMinutes;

    @Value("${attachment-storage.download-url-expiry-hours:24}")
    private int downloadUrlExpiryHours;

    @Override
    @Transactional
    public PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request) {
        validateUploadRequest(request);

        String attachmentId = generateAttachmentId();
        String extension = extractFileExtension(request.getFileName());
        String objectName = buildObjectName(attachmentId, request.getCategory(), extension);

        String uploadUrl = attachmentUrlService.generatePresignedPutUrl(
                minioProperties.getBucketName(), objectName
        );

        Attachment record = attachmentMapper.toEntity(request, attachmentId, objectName, minioProperties.getBucketName());

        attachmentRepository.save(record);

        long expiresAt = attachmentUrlService.calculateExpiryTimestamp(presignedUploadExpiryMinutes);
        Map<String, String> headers = Map.of("Content-Type", request.getContentType());

        log.info("Generated presigned upload URL. AttachmentId: {}, dimensions: {}x{}",
                attachmentId,
                request.getWidth(),
                request.getHeight());

        return attachmentMapper.toPresignedResponse(
                attachmentId,
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
        Attachment record = findAttachmentById(request.getAttachmentId());

        if (record.isConfirmed()) {
            log.warn("Attachment already confirmed: {}", request.getAttachmentId());
            return buildConfirmResponse(record);
        }

        if (!record.isPending()) {
            throw new AppException(ErrorCode.ATTACHMENT_STORAGE_ERROR, "Cannot confirm attachment with status: " + record.getStatus());
        }

        try {
            StatObjectResponse stat = verifyObjectExistsInMinio(record);

            String fileName = request.getFileName() != null ? request.getFileName() : record.getFileName();
            record.setFileSize(stat.size());
            record.setContentType(stat.contentType());
            record.setFileName(fileName);
            record.setStatus(Attachment.AttachmentStatus.CONFIRMED);
            record.setConfirmedAt(Instant.now());

            attachmentRepository.save(record);
            log.info("Upload confirmed. AttachmentId: {}", request.getAttachmentId());

            return buildConfirmResponse(record);

        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new AppException(ErrorCode.ATTACHMENT_NOT_FOUND, "Attachment file not found in storage.");
            }
            throw new AppException(ErrorCode.ATTACHMENT_STORAGE_ERROR, "Failed to verify upload", e);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to confirm upload: {}", request.getAttachmentId(), e);
            throw new AppException(ErrorCode.ATTACHMENT_STORAGE_ERROR, "Failed to confirm upload", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentUrlResponse getAttachmentUrl(String attachmentId, Integer expirySeconds) {
        Attachment record = findConfirmedAttachment(attachmentId);

        int expiry = (expirySeconds != null && expirySeconds > 0)
                ? expirySeconds
                : downloadUrlExpiryHours * 3600;

        String url = attachmentUrlService.generatePresignedGetUrl(
                record.getBucketName(), record.getObjectName(), expiry
        );
        long expiresAt = attachmentUrlService.calculateExpiryTimestampSeconds(expiry);

        return attachmentMapper.toUrlResponse(attachmentId, url, expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentMetadata getAttachmentMetadata(String attachmentId) {
        Attachment record = findConfirmedAttachment(attachmentId);
        return attachmentMapper.toMetadata(record);
    }

    @Override
    @Transactional
    public void deleteAttachment(String attachmentId) {
        Attachment record = findAttachmentById(attachmentId);

        try {
            deleteFromMinio(record.getBucketName(), record.getObjectName());

            record.setStatus(Attachment.AttachmentStatus.DELETED);
            attachmentRepository.save(record);

            log.info("Attachment deleted: {}", attachmentId);

        } catch (Exception e) {
            log.error("Failed to delete attachment: {}", attachmentId, e);
            throw new AppException(ErrorCode.ATTACHMENT_STORAGE_ERROR, "Failed to delete attachment", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentMetadata> listAttachmentsByCategory(String category) {
        List<Attachment> records = (category != null)
                ? attachmentRepository.findByCategoryAndStatus(category, Attachment.AttachmentStatus.CONFIRMED)
                : attachmentRepository.findByStatus(Attachment.AttachmentStatus.CONFIRMED);

        return attachmentMapper.toMetadataList(records);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream downloadAttachment(String attachmentId) {
        Attachment record = findConfirmedAttachment(attachmentId);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(record.getBucketName())
                            .object(record.getObjectName())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download attachment: {}", attachmentId, e);
            throw new AppException(ErrorCode.ATTACHMENT_STORAGE_ERROR, "Failed to download attachment", e);
        }
    }

    private void validateUploadRequest(PresignedUploadRequest request) {
        if (request.getFileSize() > maxFileSize) {
            throw new AppException(ErrorCode.ATTACHMENT_STORAGE_ERROR, String.format(
                    "File size %d bytes exceeds maximum %d bytes",
                    request.getFileSize(), maxFileSize
            ));
        }

        String contentType = request.getContentType().toLowerCase();
        if (!allowedContentTypes.contains(contentType)) {
            throw new AppException(ErrorCode.ATTACHMENT_STORAGE_ERROR, String.format(
                    "Content type '%s' is not allowed. Allowed: %s",
                    contentType, allowedContentTypes
            ));
        }
    }

    private ConfirmUploadResponse buildConfirmResponse(Attachment record) {
        String attachmentUrl = attachmentUrlService.generatePresignedGetUrlSafely(
                record.getBucketName(),
                record.getObjectName(),
                null
        );

        return attachmentMapper.toConfirmResponse(
                record,
                attachmentUrl
        );
    }

    private String generateAttachmentId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String buildObjectName(String attachmentId, String category, String extension) {
        StringBuilder sb = new StringBuilder();

        if (category != null && !category.isBlank()) {
            sb.append(category.toLowerCase()).append("/");
        }

        sb.append(LocalDate.now()).append("/");
        sb.append(attachmentId).append(".").append(extension);

        return sb.toString();
    }

    private Attachment findAttachmentById(String attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND, "Attachment not found: " + attachmentId));
    }

    private Attachment findConfirmedAttachment(String attachmentId) {
        return attachmentRepository.findByIdAndStatus(attachmentId, Attachment.AttachmentStatus.CONFIRMED)
                .orElseThrow(() -> new AppException(ErrorCode.ATTACHMENT_NOT_FOUND, "Confirmed attachment not found: " + attachmentId));
    }

    private StatObjectResponse verifyObjectExistsInMinio(Attachment record) throws Exception {
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

