package com.nguyenquyen.storageservice.attachment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUrlResponse {

    private String attachmentId;
    private String url;
    private Long expiresAt;
}
