package com.nguyenquyen.chatservice.chat;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.nguyenquyen.chatservice.message.Message;
import com.nguyenquyen.chatservice.message.MessageState;
import com.nguyenquyen.chatservice.message.MessageType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMapper {

    @Mapping(target = "lastMessage", expression = "java(getLastMessagePreview(chat))")
    @Mapping(target = "lastMessageTime", expression = "java(getLastMessageTime(chat))")
    @Mapping(target = "unreadCount", expression = "java(countUnreadMessages(chat, currentUserId))")
    @Mapping(target = "lastMessageType", expression = "java(getLastMessageType(chat))")
    @Mapping(target = "lastMessageImageId", expression = "java(getLastMessageImageId(chat))")
    ChatResponse toResponse(Chat chat, @Context String currentUserId);

    ChatMemberResponse toMemberResponse(ChatMember chatMember);

    default List<ChatResponse> toResponseList(List<Chat> chats, String currentUserId) {
        return chats.stream()
                .map(chat -> toResponse(chat, currentUserId))
                .toList();
    }

    default Optional<Message> getLastMessage(Chat chat) {
        if (chat.getMessages() == null || chat.getMessages().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(chat.getMessages().get(0));
    }

    default String getLastMessagePreview(Chat chat) {
        return getLastMessage(chat)
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

    default Instant getLastMessageTime(Chat chat) {
        return getLastMessage(chat)
                .map(Message::getCreatedAt)
                .orElse(chat.getCreatedAt());
    }

    default MessageType getLastMessageType(Chat chat) {
        return getLastMessage(chat)
                .map(Message::getType)
                .orElse(null);
    }

    default String getLastMessageImageId(Chat chat) {
        return getLastMessage(chat)
                .filter(m -> m.getType() == MessageType.IMAGE)
                .map(Message::getImageId)
                .orElse(null);
    }

    default int countUnreadMessages(Chat chat, String currentUserId) {
        if (chat.getMessages() == null) {
            return 0;
        }
        return (int) chat.getMessages().stream()
                .filter(m -> !m.getSenderId().equals(currentUserId))
                .filter(m -> m.getState() == MessageState.SENT)
                .count();
    }
}
