package com.nguyenquyen.storageservice.attachment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @NotNull(message = "File size is required")
    @Min(value = 1, message = "File size must be greater than 0")
    private Long fileSize;

    private String category;

    @NotNull(message = "Width is required")
    @Min(value = 1, message = "Width must be positive")
    private Integer width;

    @NotNull(message = "Height is required")
    @Min(value = 1, message = "Height must be positive")
    private Integer height;
}