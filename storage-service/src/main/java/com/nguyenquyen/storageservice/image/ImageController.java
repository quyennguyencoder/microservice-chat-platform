package com.nguyenquyen.storageservice.image;

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
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/presigned-upload")
    public ResponseEntity<ApiResponse<PresignedUploadResponse>> getPresignedUploadUrl(
            @Valid @RequestBody PresignedUploadRequest request) {
        log.info("Generating presigned upload URL for: {}", request.getFileName());
        return ResponseEntity.ok(ApiResponse.success(imageService.generatePresignedUploadUrl(request)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmUploadResponse>> confirmUpload(
            @Valid @RequestBody ConfirmUploadRequest request) {
        log.info("Confirming upload for imageId: {}", request.getImageId());
        return ResponseEntity.ok(ApiResponse.success(imageService.confirmUpload(request)));
    }

    @GetMapping("/{imageId}/url")
    public ResponseEntity<ApiResponse<ImageUrlResponse>> getImageUrl(
            @PathVariable String imageId,
            @RequestParam(value = "expiry", required = false) Integer expirySeconds) {
        return ResponseEntity.ok(ApiResponse.success(imageService.getImageUrl(imageId, expirySeconds)));
    }

    @GetMapping("/{imageId}/metadata")
    public ResponseEntity<ApiResponse<ImageMetadata>> getImageMetadata(@PathVariable String imageId) {
        return ResponseEntity.ok(ApiResponse.success(imageService.getImageMetadata(imageId)));
    }

    @GetMapping("/{imageId}")
    public void downloadImage(@PathVariable String imageId, HttpServletResponse response) {
        ImageMetadata metadata = imageService.getImageMetadata(imageId);

        response.setContentType(metadata.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + metadata.getFileName() + "\"");

        if (metadata.getSize() != null) {
            response.setContentLengthLong(metadata.getSize());
        }

        try (InputStream imageStream = imageService.downloadImage(imageId);
             OutputStream outputStream = response.getOutputStream()) {

            imageStream.transferTo(outputStream);
            outputStream.flush();

        } catch (Exception e) {
            log.error("Error downloading image: {}", imageId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String imageId) {
        log.info("Deleting image: {}", imageId);
        imageService.deleteImage(imageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<ImageMetadata>>> listImages(
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(imageService.listImagesByCategory(category)));
    }
}