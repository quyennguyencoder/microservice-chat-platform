package com.nguyenquyen.storageservice.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    Optional<Attachment> findByIdAndStatus(String attachmentId, Attachment.AttachmentStatus status);

    List<Attachment> findByStatus(Attachment.AttachmentStatus status);

    List<Attachment> findByCategory(String category);

    List<Attachment> findByCategoryAndStatus(String category, Attachment.AttachmentStatus status);

    @Query("SELECT i FROM Attachment i WHERE i.status = :status AND i.createdAt < :threshold")
    List<Attachment> findExpiredPendingUploads(
            @Param("status") Attachment.AttachmentStatus status,
            @Param("threshold") Instant threshold,
            org.springframework.data.domain.Pageable pageable
    );

}