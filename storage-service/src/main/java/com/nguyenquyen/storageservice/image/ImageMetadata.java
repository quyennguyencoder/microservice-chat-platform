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
public class ImageMetadata {

    private String imageId;
    private String fileName;
    private String contentType;
    private Long size;
    private String etag;
    private Instant updatedAt;
    private String bucketName;

    private Integer width;
    private Integer height;

    private Integer originalWidth;
    private Integer originalHeight;
}
