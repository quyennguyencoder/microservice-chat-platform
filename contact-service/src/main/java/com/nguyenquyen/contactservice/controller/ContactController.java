package com.nguyenquyen.contactservice.controller;


import com.nguyenquyen.contactservice.dto.request.SendContactRequest;
import com.nguyenquyen.contactservice.dto.response.ContactResponse;
import com.nguyenquyen.contactservice.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/contacts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping(value = "/request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContactResponse> sendContactRequest(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SendContactRequest request) {

        ContactResponse response = contactService.sendRequest(UUID.fromString(userId), request);
        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{contactId}/accept")
    public ResponseEntity<ContactResponse> acceptContactRequest(
            @PathVariable UUID contactId,
            @RequestHeader("X-User-Id") String userId) {

        ContactResponse response = contactService.acceptRequest(contactId, UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> removeContact(
            @PathVariable UUID contactId,
            @RequestHeader("X-User-Id") String userId) {

        contactService.removeContact(contactId, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }


    @GetMapping
    public ResponseEntity<Page<ContactResponse>> getMyContacts(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ContactResponse> contacts = contactService.getMyContacts(UUID.fromString(userId), page, size);
        return ResponseEntity.ok(contacts);
    }


    @GetMapping("/pending")
    public ResponseEntity<Page<ContactResponse>> getPendingRequests(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ContactResponse> requests = contactService.getPendingRequests(UUID.fromString(userId), page, size);
        return ResponseEntity.ok(requests);
    }


    @GetMapping("/sent")
    public ResponseEntity<Page<ContactResponse>> getSentRequests(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ContactResponse> requests = contactService.getSentRequests(UUID.fromString(userId), page, size);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/status/{targetUserId}")
    public ResponseEntity<Map<String, String>> getRelationshipStatus(
            @PathVariable UUID targetUserId,
            @RequestHeader("X-User-Id") String userId) {

        Map<String, String> status = contactService.getRelationshipStatus(
                UUID.fromString(userId), targetUserId);
        return ResponseEntity.ok(status);
    }
}
