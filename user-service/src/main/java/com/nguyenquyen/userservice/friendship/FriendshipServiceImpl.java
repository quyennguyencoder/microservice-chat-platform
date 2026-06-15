package com.nguyenquyen.userservice.friendship;

import com.nguyenquyen.userservice.exception.BadRequestException;
import com.nguyenquyen.userservice.exception.FriendshipException;
import com.nguyenquyen.userservice.exception.UserNotFoundException;
import com.nguyenquyen.userservice.kafka.UserEvent;
import com.nguyenquyen.userservice.kafka.UserEventPublisher;
import com.nguyenquyen.userservice.kafka.UserEventType;
import com.nguyenquyen.userservice.user.User;
import com.nguyenquyen.userservice.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    @Override
    @Transactional
    public FriendshipResponse sendFriendRequest(String targetUserId) {
        String currentUserId = getCurrentUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot send friend request to yourself");
        }

        User currentUser = getUserOrThrow(currentUserId);
        User targetUser = getUserOrThrow(targetUserId);

        // Check if any relationship already exists
        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(currentUserId, targetUserId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            switch (f.getStatus()) {
                case ACCEPTED -> throw new FriendshipException("Already friends with this user");
                case PENDING -> {
                    if (f.getRequester().getId().equals(currentUserId)) {
                        throw new FriendshipException("Friend request already sent");
                    } else {
                        // They sent us a request — auto-accept
                        return acceptExistingRequest(f);
                    }
                }
                case BLOCKED -> {
                    if (f.getRequester().getId().equals(currentUserId)) {
                        throw new FriendshipException("You have blocked this user. Unblock first.");
                    } else {
                        throw new FriendshipException("Cannot send friend request to this user");
                    }
                }
                case DECLINED -> {
                    // Allow re-requesting after decline
                    f.setRequester(currentUser);
                    f.setAddressee(targetUser);
                    f.setStatus(FriendshipStatus.PENDING);
                    friendshipRepository.save(f);

                    publishFriendEvent(currentUserId, targetUserId, UserEventType.FRIEND_REQUEST);

                    return toResponse(f, targetUser);
                }
            }
        }

        // Create new friendship request
        Friendship friendship = Friendship.builder()
                .requester(currentUser)
                .addressee(targetUser)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        publishFriendEvent(currentUserId, targetUserId, UserEventType.FRIEND_REQUEST);

        log.info("Friend request sent: {} → {}", currentUserId, targetUserId);
        return toResponse(friendship, targetUser);
    }

    @Override
    @Transactional
    public FriendshipResponse acceptFriendRequest(String requesterUserId) {
        String currentUserId = getCurrentUserId();

        Friendship friendship = friendshipRepository
                .findByRequesterIdAndAddresseeId(requesterUserId, currentUserId)
                .orElseThrow(() -> new FriendshipException("No pending friend request from this user"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipException("No pending friend request from this user");
        }

        return acceptExistingRequest(friendship);
    }

    @Override
    @Transactional
    public void declineFriendRequest(String requesterUserId) {
        String currentUserId = getCurrentUserId();

        Friendship friendship = friendshipRepository
                .findByRequesterIdAndAddresseeId(requesterUserId, currentUserId)
                .orElseThrow(() -> new FriendshipException("No pending friend request from this user"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new FriendshipException("No pending friend request from this user");
        }

        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);

        log.info("Friend request declined: {} declined {}", currentUserId, requesterUserId);
    }

    @Override
    @Transactional
    public void removeFriend(String targetUserId) {
        String currentUserId = getCurrentUserId();

        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new FriendshipException("No friendship found with this user"));

        if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
            throw new FriendshipException("Cannot remove a blocked relationship. Use unblock instead.");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship removed between {} and {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional
    public void blockUser(String targetUserId) {
        String currentUserId = getCurrentUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new BadRequestException("Cannot block yourself");
        }

        getUserOrThrow(targetUserId); // Verify target exists

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(currentUserId, targetUserId);

        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.BLOCKED && f.getRequester().getId().equals(currentUserId)) {
                throw new FriendshipException("User is already blocked");
            }
            // Overwrite existing relationship with a block
            f.setRequester(getUserOrThrow(currentUserId));
            f.setAddressee(getUserOrThrow(targetUserId));
            f.setStatus(FriendshipStatus.BLOCKED);
            friendshipRepository.save(f);
        } else {
            Friendship friendship = Friendship.builder()
                    .requester(getUserOrThrow(currentUserId))
                    .addressee(getUserOrThrow(targetUserId))
                    .status(FriendshipStatus.BLOCKED)
                    .build();
            friendshipRepository.save(friendship);
        }

        log.info("User {} blocked user {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional
    public void unblockUser(String targetUserId) {
        String currentUserId = getCurrentUserId();

        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new FriendshipException("No relationship found with this user"));

        if (friendship.getStatus() != FriendshipStatus.BLOCKED) {
            throw new FriendshipException("User is not blocked");
        }

        if (!friendship.getRequester().getId().equals(currentUserId)) {
            throw new FriendshipException("You did not block this user");
        }

        friendshipRepository.delete(friendship);
        log.info("User {} unblocked user {}", currentUserId, targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getFriends() {
        String currentUserId = getCurrentUserId();
        return friendshipRepository.findAcceptedFriendships(currentUserId)
                .stream()
                .map(f -> {
                    User friend = f.getRequester().getId().equals(currentUserId)
                            ? f.getAddressee()
                            : f.getRequester();
                    return toResponse(f, friend);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getIncomingRequests() {
        String currentUserId = getCurrentUserId();
        return friendshipRepository.findByAddresseeIdAndStatus(currentUserId, FriendshipStatus.PENDING)
                .stream()
                .map(f -> toResponse(f, f.getRequester()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getOutgoingRequests() {
        String currentUserId = getCurrentUserId();
        return friendshipRepository.findByRequesterIdAndStatus(currentUserId, FriendshipStatus.PENDING)
                .stream()
                .map(f -> toResponse(f, f.getAddressee()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FriendshipCheckResponse checkFriendship(String targetUserId) {
        String currentUserId = getCurrentUserId();

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(currentUserId, targetUserId);

        if (existing.isEmpty()) {
            return new FriendshipCheckResponse(targetUserId, null, false, false, false, false);
        }

        Friendship f = existing.get();
        boolean isFriend = f.getStatus() == FriendshipStatus.ACCEPTED;
        boolean isPendingIncoming = f.getStatus() == FriendshipStatus.PENDING
                && f.getAddressee().getId().equals(currentUserId);
        boolean isPendingOutgoing = f.getStatus() == FriendshipStatus.PENDING
                && f.getRequester().getId().equals(currentUserId);
        boolean isBlocked = f.getStatus() == FriendshipStatus.BLOCKED;

        return new FriendshipCheckResponse(
                targetUserId, f.getStatus(), isFriend, isPendingIncoming, isPendingOutgoing, isBlocked
        );
    }

    // ===================== Private helpers =====================

    private FriendshipResponse acceptExistingRequest(Friendship friendship) {
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        String accepterId = friendship.getAddressee().getId();
        String requesterId = friendship.getRequester().getId();

        publishFriendEvent(accepterId, requesterId, UserEventType.FRIEND_ACCEPTED);

        log.info("Friend request accepted: {} accepted {}", accepterId, requesterId);

        User requesterUser = friendship.getRequester();
        return toResponse(friendship, requesterUser);
    }

    private FriendshipResponse toResponse(Friendship friendship, User otherUser) {
        return new FriendshipResponse(
                otherUser.getId(),
                otherUser.getUsername(),
                otherUser.getDisplayName(),
                otherUser.getImageId(),
                friendship.getStatus(),
                friendship.getCreatedAt()
        );
    }

    private void publishFriendEvent(String actorId, String recipientId, UserEventType eventType) {
        eventPublisher.publish(UserEvent.builder()
                .type(eventType.name())
                .actorId(actorId)
                .recipientId(recipientId)
                .userId(actorId)
                .build());
    }

    private User getUserOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private String getCurrentUserId() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        Jwt jwt = auth.getToken();
        return jwt.getSubject();
    }
}
