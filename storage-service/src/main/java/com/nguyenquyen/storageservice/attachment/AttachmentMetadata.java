package com.nguyenquyen.storageservice.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentMetadata {

    private String attachmentId;
    private String fileName;
    private String contentType;
    private Long size;
    private Instant updatedAt;
    private String bucketName;
    private Integer width;
    private Integer height;

}
