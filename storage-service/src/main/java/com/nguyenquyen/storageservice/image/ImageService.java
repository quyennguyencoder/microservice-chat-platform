package com.nguyenquyen.storageservice.image;

import java.io.InputStream;
import java.util.List;

public interface ImageService {

    PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request);

    ConfirmUploadResponse confirmUpload(ConfirmUploadRequest request);

    ImageUrlResponse getImageUrl(String imageId, Integer expirySeconds);

    ImageMetadata getImageMetadata(String imageId);

    void deleteImage(String imageId);

    List<ImageMetadata> listImagesByCategory(String category);

    InputStream downloadImage(String imageId);
}

