package com.nguyenquyen.chatservice.chat.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.nguyenquyen.chatservice.chat.entity.Chat;
import com.nguyenquyen.chatservice.chat.entity.ChatMember;
import com.nguyenquyen.chatservice.chat.enums.ChatMemberRole;
import com.nguyenquyen.chatservice.chat.dto.ChatResponse;
import com.nguyenquyen.chatservice.chat.dto.ChatMemberResponse;
import com.nguyenquyen.chatservice.message.entity.Message;
import com.nguyenquyen.chatservice.message.repository.MessageRepository;
import com.nguyenquyen.chatservice.message.enums.MessageState;
import com.nguyenquyen.chatservice.message.enums.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMapper {

    @Mapping(target = "lastMessage", expression = "java(getLastMessagePreview(chat, messageRepository))")
    @Mapping(target = "lastMessageTime", expression = "java(getLastMessageTime(chat, messageRepository))")
    @Mapping(target = "unreadCount", expression = "java(countUnreadMessages(chat, currentUserId, messageRepository))")
    @Mapping(target = "lastMessageType", expression = "java(getLastMessageType(chat, messageRepository))")
    @Mapping(target = "lastMessageAttachmentId", expression = "java(getLastMessageAttachmentId(chat, messageRepository))")
    ChatResponse toResponse(Chat chat, @Context String currentUserId, @Context MessageRepository messageRepository);

    ChatMemberResponse toMemberResponse(ChatMember chatMember);



    default Optional<Message> getLastMessage(Chat chat, MessageRepository messageRepository) {
        return messageRepository.findFirstByChatIdOrderByCreatedAtDesc(chat.getId());
    }

    default String getLastMessagePreview(Chat chat, MessageRepository messageRepository) {
        return getLastMessage(chat, messageRepository)
                .map(message -> {
                    if (message.getType() != MessageType.TEXT && message.getType() != MessageType.SYSTEM) {
                        return "📎 " + message.getType().name().toLowerCase();
                    }
                    String content = message.getContent();
                    if (content == null || content.isEmpty()) {
                        return null;
                    }
                    return content.length() > 50
                            ? content.substring(0, 50) + "..."
                            : content;
                })
                .orElse(null);
    }

    default Instant getLastMessageTime(Chat chat, MessageRepository messageRepository) {
        return getLastMessage(chat, messageRepository)
                .map(Message::getCreatedAt)
                .orElse(chat.getCreatedAt());
    }

    default MessageType getLastMessageType(Chat chat, MessageRepository messageRepository) {
        return getLastMessage(chat, messageRepository)
                .map(Message::getType)
                .orElse(null);
    }

    default String getLastMessageAttachmentId(Chat chat, MessageRepository messageRepository) {
        return getLastMessage(chat, messageRepository)
                .filter(m -> m.getType() == MessageType.IMAGE)
                .map(Message::getAttachmentId)
                .orElse(null);
    }

    default int countUnreadMessages(Chat chat, String currentUserId, MessageRepository messageRepository) {
        return messageRepository.countUnreadMessages(chat.getId(), currentUserId, MessageState.SENT);
    }

    @Mapping(target = "id", source = "chat.id")
    @Mapping(target = "type", source = "chat.type")
    @Mapping(target = "attachmentId", source = "chat.attachmentId")
    @Mapping(target = "createdAt", source = "chat.createdAt")
    @Mapping(target = "lastMessage", expression = "java(getLastMessagePreview(lastMessage))")
    @Mapping(target = "lastMessageTime", expression = "java(lastMessage != null ? lastMessage.getCreatedAt() : chat.getCreatedAt())")
    @Mapping(target = "unreadCount", source = "unreadCount")
    @Mapping(target = "lastMessageType", expression = "java(lastMessage != null ? lastMessage.getType() : null)")
    @Mapping(target = "lastMessageAttachmentId", expression = "java(lastMessage != null && lastMessage.getType() == MessageType.IMAGE ? lastMessage.getAttachmentId() : null)")
    ChatResponse toResponseBulk(Chat chat, @Context String currentUserId, Message lastMessage, int unreadCount);

    default String getLastMessagePreview(Message message) {
        if (message == null) return null;
        if (message.getType() != MessageType.TEXT && message.getType() != MessageType.SYSTEM) {
            return "📎 " + message.getType().name().toLowerCase();
        }
        String content = message.getContent();
        if (content == null || content.isEmpty()) {
            return null;
        }
        return content.length() > 50
                ? content.substring(0, 50) + "..."
                : content;
    }
}
