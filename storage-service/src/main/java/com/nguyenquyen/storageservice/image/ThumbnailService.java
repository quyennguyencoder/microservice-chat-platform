package com.nguyenquyen.storageservice.image;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import com.nguyenquyen.storageservice.exception.ImageStorageException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private static final int DEFAULT_THUMBNAIL_WIDTH = 236;
    private static final double DEFAULT_QUALITY = 0.85;

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    @PostConstruct
    public void init() {
        String[] readers = ImageIO.getReaderFormatNames();
        String[] writers = ImageIO.getWriterFormatNames();
        log.info("Supported image readers: {}", Arrays.toString(readers));
        log.info("Supported image writers: {}", Arrays.toString(writers));
    }

    public boolean isFormatSupported(String contentType) {
        return contentType != null && SUPPORTED_FORMATS.contains(contentType.toLowerCase());
    }


    public int calculateThumbnailHeight(int originalWidth, int originalHeight, int thumbnailWidth) {
        if (originalWidth <= 0 || originalHeight <= 0) {
            throw new IllegalArgumentException("Original dimensions must be positive");
        }

        double aspectRatio = (double) originalHeight / originalWidth;
        return (int) Math.round(thumbnailWidth * aspectRatio);
    }

    public ThumbnailResult generateThumbnail(
            InputStream originalImage,
            int originalWidth,
            int originalHeight,
            Integer targetWidth
    ) {
        int thumbWidth = targetWidth != null && targetWidth > 0 ? targetWidth : DEFAULT_THUMBNAIL_WIDTH;
        int thumbHeight = calculateThumbnailHeight(originalWidth, originalHeight, thumbWidth);

        BufferedImage bufferedImage = null;

        try {
            bufferedImage = ImageIO.read(originalImage);

            if (bufferedImage == null) {
                throw new ImageStorageException(
                        "Cannot read image. Format may not be supported.");
            }

            log.debug("Generating thumbnail: {}x{} -> {}x{}",
                    originalWidth, originalHeight, thumbWidth, thumbHeight);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Thumbnails.of(bufferedImage)
                        .size(thumbWidth, thumbHeight)
                        .keepAspectRatio(true)
                        .outputFormat("jpg")
                        .outputQuality(DEFAULT_QUALITY)
                        .toOutputStream(outputStream);

                byte[] thumbnailBytes = outputStream.toByteArray();

                log.debug("Generated thumbnail: {}x{}, size: {} bytes",
                        thumbWidth, thumbHeight, thumbnailBytes.length);

                return new ThumbnailResult(thumbnailBytes, thumbWidth, thumbHeight);
            }

        } catch (ImageStorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            throw new ImageStorageException("Failed to generate thumbnail: " + e.getMessage(), e);
        } finally {
            if (bufferedImage != null) {
                bufferedImage.flush();
            }
        }
    }
}
