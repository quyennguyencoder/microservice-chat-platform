package com.nguyenquyen.contactservice.repository;

import com.nguyenquyen.contactservice.entity.Contact;
import com.nguyenquyen.contactservice.enums.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    @Query("""
        SELECT c FROM Contact c
        WHERE (c.requesterId = :userA AND c.addresseeId = :userB)
           OR (c.requesterId = :userB AND c.addresseeId = :userA)
        """)
    Optional<Contact> findBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);


    @Query("""
        SELECT c FROM Contact c
        WHERE (c.requesterId = :userId OR c.addresseeId = :userId)
          AND c.status = :status
        ORDER BY c.updatedAt DESC
        """)
    Page<Contact> findAllByUserAndStatus(
            @Param("userId") UUID userId,
            @Param("status") ContactStatus status,
            Pageable pageable);


    Page<Contact> findByAddresseeIdAndStatus(UUID addresseeId, ContactStatus status, Pageable pageable);


    Page<Contact> findByRequesterIdAndStatus(UUID requesterId, ContactStatus status, Pageable pageable);


    @Query("""
        SELECT COUNT(c) > 0 FROM Contact c
        WHERE (c.requesterId = :userA AND c.addresseeId = :userB)
           OR (c.requesterId = :userB AND c.addresseeId = :userA)
        """)
    boolean existsBetweenUsers(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
