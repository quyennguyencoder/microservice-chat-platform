package com.nguyenquyen.storageservice.image;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadResponse {

    private String imageId;
    private String imageUrl;
    private String thumbnailUrl;
    private String fileName;
    private Long size;
    private String contentType;
    private Instant updatedAt;
    private boolean confirmed;

    private Integer originalWidth;
    private Integer originalHeight;

    private Integer thumbnailWidth;
    private Integer thumbnailHeight;
}