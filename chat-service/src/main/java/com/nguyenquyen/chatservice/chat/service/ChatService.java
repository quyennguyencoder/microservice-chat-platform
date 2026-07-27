package com.nguyenquyen.chatservice.chat.service;

import com.nguyenquyen.chatservice.chat.dto.ChatMemberResponse;
import com.nguyenquyen.chatservice.chat.entity.Chat;
import com.nguyenquyen.chatservice.chat.dto.AddMembersRequest;
import com.nguyenquyen.chatservice.chat.dto.ChangeRoleRequest;
import com.nguyenquyen.chatservice.chat.dto.ChatMemberResponse;
import com.nguyenquyen.chatservice.chat.entity.Chat;
import com.nguyenquyen.chatservice.chat.dto.ChatResponse;
import com.nguyenquyen.chatservice.chat.dto.CreateGroupRequest;
import com.nguyenquyen.chatservice.chat.dto.UpdateGroupRequest;
import java.util.List;
import java.util.UUID;

public interface ChatService {

    ChatResponse getOrCreatePrivateChat(String targetUserId);

    ChatResponse createGroupChat(CreateGroupRequest request);

    ChatResponse updateGroupChat(UUID chatId, UpdateGroupRequest request);

    ChatResponse addMembers(UUID chatId, AddMembersRequest request);

    void removeMember(UUID chatId, String memberId);

    ChatResponse changeMemberRole(UUID chatId, String targetUserId, ChangeRoleRequest request);

    void leaveChat(UUID chatId);

    List<ChatResponse> getMyChats();

    ChatResponse getChatById(UUID chatId);

    void deleteChat(UUID chatId);

    Chat getChatEntityById(UUID chatId);
}