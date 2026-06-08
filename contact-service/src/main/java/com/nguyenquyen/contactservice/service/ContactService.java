package com.nguyenquyen.contactservice.service;

import com.nguyenquyen.contactservice.dto.response.ContactResponse;
import com.nguyenquyen.contactservice.dto.request.SendContactRequest;
import com.nguyenquyen.contactservice.enums.ContactStatus;
import com.nguyenquyen.contactservice.exception.ContactAccessDeniedException;
import com.nguyenquyen.contactservice.exception.ContactAlreadyExistsException;
import com.nguyenquyen.contactservice.exception.ContactNotFoundException;
import com.nguyenquyen.contactservice.entity.Contact;
import com.nguyenquyen.contactservice.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;

    // ─── Send Contact Request ──────────────────────────────────────────────────

    /**
     * Creates a new PENDING contact request from the requester to the addressee.
     *
     * @param requesterId UUID of the authenticated user sending the request
     * @param request     body containing the target addresseeId
     * @return the created contact record
     * @throws IllegalArgumentException      if requester tries to add themselves
     * @throws ContactAlreadyExistsException if a relationship already exists
     */
    @Transactional
    public ContactResponse sendRequest(UUID requesterId, SendContactRequest request) {
        UUID addresseeId = request.getAddresseeId();

        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("You cannot send a contact request to yourself");
        }

        if (contactRepository.existsBetweenUsers(requesterId, addresseeId)) {
            throw new ContactAlreadyExistsException(
                "A contact relationship already exists with this user");
        }

        Contact contact = Contact.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .status(ContactStatus.PENDING)
                .build();

        Contact saved = contactRepository.save(contact);
        log.debug("Contact request sent: {} → {}", requesterId, addresseeId);
        return toResponse(saved);
    }

    // ─── Accept Request ────────────────────────────────────────────────────────

    /**
     * Accepts a PENDING contact request.
     * Only the addressee (recipient) of the request can accept it.
     *
     * @param contactId  UUID of the contact record to accept
     * @param addresseeId UUID of the authenticated user accepting the request
     * @return the updated contact record with status ACCEPTED
     * @throws ContactNotFoundException      if no record found for the given ID
     * @throws ContactAccessDeniedException  if the caller is not the addressee
     * @throws IllegalArgumentException      if the contact is not in PENDING status
     */
    @Transactional
    public ContactResponse acceptRequest(UUID contactId, UUID addresseeId) {
        Contact contact = findOrThrow(contactId);

        if (!contact.getAddresseeId().equals(addresseeId)) {
            throw new ContactAccessDeniedException("Only the addressee can accept this request");
        }
        if (contact.getStatus() != ContactStatus.PENDING) {
            throw new IllegalArgumentException("Contact request is not in PENDING status");
        }

        contact.setStatus(ContactStatus.ACCEPTED);
        Contact saved = contactRepository.save(contact);
        log.debug("Contact request accepted: contactId={}", contactId);
        return toResponse(saved);
    }

    // ─── Reject / Remove Contact ──────────────────────────────────────────────

    /**
     * Rejects a PENDING request (addressee action) or removes an ACCEPTED contact.
     * Can also be used by the requester to cancel their own outgoing request.
     *
     * @param contactId UUID of the contact record
     * @param userId    UUID of the authenticated user performing the action
     * @throws ContactNotFoundException     if no record found for the given ID
     * @throws ContactAccessDeniedException if the caller is not a participant
     */
    @Transactional
    public void removeContact(UUID contactId, UUID userId) {
        Contact contact = findOrThrow(contactId);

        boolean isParticipant = contact.getRequesterId().equals(userId)
                || contact.getAddresseeId().equals(userId);

        if (!isParticipant) {
            throw new ContactAccessDeniedException("You are not a participant in this contact relationship");
        }

        contactRepository.delete(contact);
        log.debug("Contact removed: contactId={} by userId={}", contactId, userId);
    }

    // ─── List Contacts (paginated) ─────────────────────────────────────────────

    /**
     * Returns a paginated list of accepted contacts for the authenticated user.
     *
     * @param userId UUID of the authenticated user
     * @param page   zero-based page index
     * @param size   page size (max results per page)
     * @return page of accepted contact records
     */
    public Page<ContactResponse> getMyContacts(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return contactRepository
                .findAllByUserAndStatus(userId, ContactStatus.ACCEPTED, pageable)
                .map(this::toResponse);
    }

    /**
     * Returns a paginated list of PENDING contact requests received by the user.
     *
     * @param userId UUID of the authenticated user (as addressee)
     * @param page   zero-based page index
     * @param size   page size
     * @return page of incoming pending requests
     */
    public Page<ContactResponse> getPendingRequests(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return contactRepository
                .findByAddresseeIdAndStatus(userId, ContactStatus.PENDING, pageable)
                .map(this::toResponse);
    }

    /**
     * Returns a paginated list of PENDING contact requests sent by the user.
     *
     * @param userId UUID of the authenticated user (as requester)
     * @param page   zero-based page index
     * @param size   page size
     * @return page of outgoing pending requests
     */
    public Page<ContactResponse> getSentRequests(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return contactRepository
                .findByRequesterIdAndStatus(userId, ContactStatus.PENDING, pageable)
                .map(this::toResponse);
    }

    // ─── Status Check ──────────────────────────────────────────────────────────

    /**
     * Returns the relationship status between the authenticated user and a target user.
     *
     * @param userId       UUID of the authenticated user
     * @param targetUserId UUID of the target user to check against
     * @return map with "status" key: NONE, PENDING, ACCEPTED, or PENDING_SENT
     */
    public Map<String, String> getRelationshipStatus(UUID userId, UUID targetUserId) {
        return contactRepository.findBetweenUsers(userId, targetUserId)
                .map(c -> {
                    if (c.getStatus() == ContactStatus.ACCEPTED) {
                        return Map.of("status", "ACCEPTED");
                    }
                    // PENDING — differentiate direction
                    if (c.getRequesterId().equals(userId)) {
                        return Map.of("status", "PENDING_SENT");
                    }
                    return Map.of("status", "PENDING_RECEIVED");
                })
                .orElse(Map.of("status", "NONE"));
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private Contact findOrThrow(UUID contactId) {
        return contactRepository.findById(contactId)
                .orElseThrow(() -> new ContactNotFoundException(
                    "Contact not found with id: " + contactId));
    }

    private ContactResponse toResponse(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .requesterId(contact.getRequesterId())
                .addresseeId(contact.getAddresseeId())
                .status(contact.getStatus())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }
}
