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
    @Mapping(target = "width", source = "request.width")
    @Mapping(target = "height", source = "request.height")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    Image toEntity(PresignedUploadRequest request, String imageId, String objectName, String bucketName);

    @Mapping(target = "imageId", source = "id")
    @Mapping(target = "size", source = "fileSize")
    @Mapping(target = "width", source = "width")
    @Mapping(target = "height", source = "height")
    ImageMetadata toMetadata(Image image);

    List<ImageMetadata> toMetadataList(List<Image> images);

    @Mapping(target = "imageId", source = "image.id")
    @Mapping(target = "size", source = "image.fileSize")
    @Mapping(target = "confirmed", expression = "java(image.isConfirmed())")
    @Mapping(target = "width", source = "image.width")
    @Mapping(target = "height", source = "image.height")
    ConfirmUploadResponse toConfirmResponse(
            Image image,
            String imageUrl
    );

    default PresignedUploadResponse toPresignedResponse(
            String imageId,
            String uploadUrl,
            long expiresAt,
            Map<String, String> requiredHeaders,
            Integer width,
            Integer height
    ) {
        return PresignedUploadResponse.builder()
                .imageId(imageId)
                .uploadUrl(uploadUrl)
                .expiresAt(expiresAt)
                .requiredHeaders(requiredHeaders)
                .width(width)
                .height(height)
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