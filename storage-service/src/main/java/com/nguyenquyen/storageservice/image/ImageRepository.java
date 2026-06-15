package com.nguyenquyen.storageservice.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {

    Optional<Image> findByIdAndStatus(String imageId, Image.ImageStatus status);

    List<Image> findByStatus(Image.ImageStatus status);

    List<Image> findByCategory(String category);

    List<Image> findByCategoryAndStatus(String category, Image.ImageStatus status);

    @Query("SELECT i FROM Image i WHERE i.status = :status AND i.createdAt < :threshold")
    List<Image> findExpiredPendingUploads(
            @Param("status") Image.ImageStatus status,
            @Param("threshold") Instant threshold
    );

    @Query("SELECT i FROM Image i WHERE i.thumbnailImageId = :thumbnailId")
    Optional<Image> findByThumbnailImageId(@Param("thumbnailId") String thumbnailId);
}