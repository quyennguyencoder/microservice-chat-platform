package com.nguyenquyen.storageservice.image;

import org.mapstruct.*;
import java.util.List;
import java.util.Map;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ImageMapper {

    @Mapping(target = "id", source = "imageId")
    @Mapping(target = "objectName", source = "objectName")
    @Mapping(target = "bucketName", source = "bucketName")
    @Mapping(target = "fileName", source = "request.fileName")
    @Mapping(target = "contentType", source = "request.contentType")
    @Mapping(target = "fileSize", source = "request.fileSize")
    @Mapping(target = "category", source = "request.category")
    @Mapping(target = "width", source = "request.originalWidth")
    @Mapping(target = "height", source = "request.originalHeight")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "thumbnailImageId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    Image toEntity(PresignedUploadRequest request, String imageId, String objectName, String bucketName);

    @Mapping(target = "imageId", source = "id")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "etag", ignore = true)
    ImageMetadata toMetadata(Image image);

    List<ImageMetadata> toMetadataList(List<Image> images);

    @Mapping(target = "imageId", source = "image.id")
    @Mapping(target = "size", source = "image.fileSize")
    @Mapping(target = "confirmed", expression = "java(image.isConfirmed())")
    @Mapping(target = "originalWidth", source = "image.width")
    @Mapping(target = "originalHeight", source = "image.height")
    @Mapping(target = "thumbnailWidth", source = "thumbnailWidth")
    @Mapping(target = "thumbnailHeight", source = "thumbnailHeight")
    ConfirmUploadResponse toConfirmResponse(
            Image image,
            String imageUrl,
            String thumbnailUrl,
            Integer thumbnailWidth,
            Integer thumbnailHeight
    );

    default PresignedUploadResponse toPresignedResponse(
            String imageId,
            String uploadUrl,
            long expiresAt,
            Map<String, String> requiredHeaders,
            String thumbnailImageId,
            Integer originalWidth,
            Integer originalHeight,
            Integer thumbnailWidth,
            Integer thumbnailHeight
    ) {
        return PresignedUploadResponse.builder()
                .imageId(imageId)
                .uploadUrl(uploadUrl)
                .expiresAt(expiresAt)
                .requiredHeaders(requiredHeaders)
                .thumbnailImageId(thumbnailImageId)
                .originalWidth(originalWidth)
                .originalHeight(originalHeight)
                .thumbnailWidth(thumbnailWidth)
                .thumbnailHeight(thumbnailHeight)
                .build();
    }

    default ImageUrlResponse toUrlResponse(String imageId, String url, long expiresAt) {
        return ImageUrlResponse.builder()
                .imageId(imageId)
                .url(url)
                .expiresAt(expiresAt)
                .build();
    }
}