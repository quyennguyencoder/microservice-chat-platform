package com.nguyenquyen.storageservice.image;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadResponse {

    private String imageId;
    private String uploadUrl;
    private Long expiresAt;
    private Map<String, String> requiredHeaders;
    private String thumbnailImageId;

    private Integer originalWidth;
    private Integer originalHeight;

    private Integer thumbnailWidth;
    private Integer thumbnailHeight;
}