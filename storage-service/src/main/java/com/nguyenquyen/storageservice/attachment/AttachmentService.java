package com.nguyenquyen.storageservice.attachment;

import java.io.InputStream;
import java.util.List;

public interface AttachmentService {

    PresignedUploadResponse generatePresignedUploadUrl(PresignedUploadRequest request);

    ConfirmUploadResponse confirmUpload(ConfirmUploadRequest request);

    AttachmentUrlResponse getAttachmentUrl(String attachmentId, Integer expirySeconds);

    AttachmentMetadata getAttachmentMetadata(String attachmentId);

    void deleteAttachment(String attachmentId);

    List<AttachmentMetadata> listAttachmentsByCategory(String category);

    InputStream downloadAttachment(String attachmentId);
}

