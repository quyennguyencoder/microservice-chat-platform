package com.nguyenquyen.storageservice.attachment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadRequest {

    @NotBlank(message = "Attachment ID is required")
    private String attachmentId;
    private String fileName;
    private String contentType;
    private Long fileSize;
}