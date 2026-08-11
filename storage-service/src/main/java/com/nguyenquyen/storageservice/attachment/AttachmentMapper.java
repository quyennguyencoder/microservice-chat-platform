package com.nguyenquyen.storageservice.attachment;

import org.mapstruct.*;
import java.util.List;
import java.util.Map;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AttachmentMapper {

    @Mapping(target = "id", source = "attachmentId")
    @Mapping(target = "objectName", source = "objectName")
    @Mapping(target = "bucketName", source = "bucketName")
    @Mapping(target = "fileName", source = "request.fileName")
    @Mapping(target = "contentType", source = "request.contentType")
    @Mapping(target = "fileSize", source = "request.fileSize")
    @Mapping(target = "category", source = "request.category")
    @Mapping(target = "width", source = "request.width")
    @Mapping(target = "height", source = "request.height")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    Attachment toEntity(PresignedUploadRequest request, String attachmentId, String objectName, String bucketName);

    @Mapping(target = "attachmentId", source = "id")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "width", source = "width")
    @Mapping(target = "height", source = "height")
    AttachmentMetadata toMetadata(Attachment attachment);

    List<AttachmentMetadata> toMetadataList(List<Attachment> attachments);

    @Mapping(target = "attachmentId", source = "attachment.id")
    @Mapping(target = "size", source = "attachment.fileSize")
    @Mapping(target = "confirmed", expression = "java(attachment.isConfirmed())")
    @Mapping(target = "width", source = "attachment.width")
    @Mapping(target = "height", source = "attachment.height")
    ConfirmUploadResponse toConfirmResponse(
            Attachment attachment,
            String attachmentUrl
    );

    default PresignedUploadResponse toPresignedResponse(
            String attachmentId,
            String uploadUrl,
            long expiresAt,
            Map<String, String> requiredHeaders,
            Integer width,
            Integer height
    ) {
        return PresignedUploadResponse.builder()
                .attachmentId(attachmentId)
                .uploadUrl(uploadUrl)
                .expiresAt(expiresAt)
                .requiredHeaders(requiredHeaders)
                .width(width)
                .height(height)
                .build();
    }

    default AttachmentUrlResponse toUrlResponse(String attachmentId, String url, long expiresAt) {
        return AttachmentUrlResponse.builder()
                .attachmentId(attachmentId)
                .url(url)
                .expiresAt(expiresAt)
                .build();
    }
}