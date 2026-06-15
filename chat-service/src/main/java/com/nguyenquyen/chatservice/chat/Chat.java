package com.nguyenquyen.chatservice.chat;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.nguyenquyen.chatservice.message.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatType type;

    @Column(length = 100)
    private String name; // Group name (null for PRIVATE)

    @Column(name = "image_id", length = 100)
    private String imageId; // Group avatar (null for PRIVATE)

    @Column(name = "owner_id", nullable = false)
    private String ownerId; // Creator of the chat

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)   
    private Instant updatedAt;

    // ===== Helper methods =====

    public boolean isMember(String userId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(userId));
    }

    public boolean isNotMember(String userId) {
        return !isMember(userId);
    }

    public boolean isPrivate() {
        return type == ChatType.PRIVATE;
    }

    public boolean isGroup() {
        return type == ChatType.GROUP;
    }

    /**
     * Get the other participant's ID in a PRIVATE chat.
     */
    public String getOtherMemberId(String userId) {
        return members.stream()
                .map(ChatMember::getUserId)
                .filter(id -> !id.equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all member user IDs.
     */
    public List<String> getMemberIds() {
        return members.stream()
                .map(ChatMember::getUserId)
                .toList();
    }

    public void addMember(ChatMember member) {
        members.add(member);
        member.setChat(this);
    }
}
