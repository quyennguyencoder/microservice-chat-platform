package com.nguyenquyen.storageservice.attachment;

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

    private String attachmentId;
    private String uploadUrl;
    private Long expiresAt;
    private Map<String, String> requiredHeaders;
    private Integer width;
    private Integer height;

}