package com.nguyenquyen.storageservice.image;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadRequest {

    @NotBlank(message = "Image ID is required")
    private String imageId;
    private String fileName;
    private String contentType;
    private Long fileSize;
}