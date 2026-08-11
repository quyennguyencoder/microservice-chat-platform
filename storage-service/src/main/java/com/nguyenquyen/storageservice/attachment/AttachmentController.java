package com.nguyenquyen.storageservice.attachment;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nguyenquyen.common.dto.response.ApiResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/presigned-upload")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> getPresignedUploadUrl(
            @Valid @RequestBody PresignedUploadRequest request) {
        log.info("Generating presigned upload URL for: {}", request.getFileName());
        return ResponseEntity.ok(ApiResponse.success(attachmentService.generatePresignedUploadUrl(request)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmUploadResponse>> confirmUpload(
            @Valid @RequestBody ConfirmUploadRequest request) {
        log.info("Confirming upload for attachmentId: {}", request.getAttachmentId());
        return ResponseEntity.ok(ApiResponse.success(attachmentService.confirmUpload(request)));
    }

    @GetMapping("/{attachmentId}/url")
    public ResponseEntity<ApiResponse<AttachmentUrlResponse>> getAttachmentUrl(
            @PathVariable String attachmentId,
            @RequestParam(value = "expiry", required = false) Integer expirySeconds) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.getAttachmentUrl(attachmentId, expirySeconds)));
    }

    @GetMapping("/{attachmentId}/metadata")
    public ResponseEntity<ApiResponse<AttachmentMetadata>> getAttachmentMetadata(@PathVariable String attachmentId) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.getAttachmentMetadata(attachmentId)));
    }

    @GetMapping("/{attachmentId}")
    public void downloadAttachment(@PathVariable String attachmentId, HttpServletResponse response) {
        AttachmentMetadata metadata = attachmentService.getAttachmentMetadata(attachmentId);

        response.setContentType(metadata.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + metadata.getFileName() + "\"");

        if (metadata.getSize() != null) {
            response.setContentLengthLong(metadata.getSize());
        }

        try (InputStream attachmentStream = attachmentService.downloadAttachment(attachmentId);
             OutputStream outputStream = response.getOutputStream()) {

            attachmentStream.transferTo(outputStream);
            outputStream.flush();

        } catch (Exception e) {
            log.error("Error downloading attachment: {}", attachmentId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable String attachmentId) {
        log.info("Deleting attachment: {}", attachmentId);
        attachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<AttachmentMetadata>>> listAttachments(
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(attachmentService.listAttachmentsByCategory(category)));
    }
}