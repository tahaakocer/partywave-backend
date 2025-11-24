package com.partywave.backend.service;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.RoomMember;
import com.partywave.backend.domain.Vote;
import com.partywave.backend.domain.enumeration.VoteType;
import com.partywave.backend.exception.ForbiddenException;
import com.partywave.backend.exception.InvalidRequestException;
import com.partywave.backend.exception.ResourceNotFoundException;
import com.partywave.backend.repository.AppUserRepository;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.repository.RoomRepository;
import com.partywave.backend.repository.VoteRepository;
import com.partywave.backend.service.dto.KickUserRequestDTO;
import com.partywave.backend.service.dto.VoteResponseDTO;
import com.partywave.backend.service.dto.VoteStatusResponseDTO;
import com.partywave.backend.service.redis.OnlineMembersRedisService;
import com.partywave.backend.service.redis.PlaybackRedisService;
import com.partywave.backend.service.redis.PlaylistRedisService;
import com.partywave.backend.service.redis.TrackOperationResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing vote operations (skip track, kick user).
 * Based on PROJECT_OVERVIEW.md sections 2.8 and 2.9 - Vote-Based Skip/Kick.
 *
 * Business rules:
 * - Users can vote once per track (skip) or per user (kick)
 * - Threshold is 50% of online members
 * - Room owner cannot be kicked
 * - User cannot vote to kick themselves
 * - When threshold is reached, action is executed immediately
 */
@Service
@Transactional
public class VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteService.class);

    // Vote threshold: 50% of online members
    private static final double VOTE_THRESHOLD_PERCENTAGE = 0.5;

    private final VoteRepository voteRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final AppUserRepository appUserRepository;
    private final OnlineMembersRedisService onlineMembersRedisService;
    private final PlaybackRedisService playbackRedisService;
    private final PlaylistRedisService playlistRedisService;

    public VoteService(
        VoteRepository voteRepository,
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        AppUserRepository appUserRepository,
        OnlineMembersRedisService onlineMembersRedisService,
        PlaybackRedisService playbackRedisService,
        PlaylistRedisService playlistRedisService
    ) {
        this.voteRepository = voteRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.appUserRepository = appUserRepository;
        this.onlineMembersRedisService = onlineMembersRedisService;
        this.playbackRedisService = playbackRedisService;
        this.playlistRedisService = playlistRedisService;
    }

    /**
     * Vote to skip the currently playing track.
     * Based on PROJECT_OVERVIEW.md section 2.8 - Skipping Tracks (Vote-Based).
     *
     * Workflow:
     * 1. Validate user is a room member
     * 2. Check there is a track currently playing
     * 3. Check if user already voted for this track
     * 4. Create vote record in PostgreSQL
     * 5. Count votes for this track
     * 6. Get online member count from Redis
     * 7. Check threshold (50%)
     * 8. If threshold reached, skip track via PlaybackRedisService
     *
     * @param roomId Room UUID
     * @param userId User UUID (from JWT)
     * @return VoteResponseDTO with vote status and whether threshold was reached
     * @throws ResourceNotFoundException if room doesn't exist
     * @throws ForbiddenException if user is not a room member
     * @throws InvalidRequestException if no track is playing or user already voted
     */
    public VoteResponseDTO voteSkipTrack(UUID roomId, UUID userId) {
        log.debug("Vote to skip track in room {} by user {}", roomId, userId);

        // Step 1: Validate room exists
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // Step 1: Validate user is a room member
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this room");
        }

        // Step 2: Check there is a track currently playing
        String currentPlaylistItemId = playbackRedisService.getCurrentPlaylistItemId(roomId.toString());
        if (currentPlaylistItemId == null) {
            throw new InvalidRequestException("No track is currently playing");
        }

        // Step 3: Check if user already voted for this track
        boolean alreadyVoted = voteRepository.existsByRoomIdAndVoterIdAndPlaylistItemId(roomId, userId, currentPlaylistItemId);
        if (alreadyVoted) {
            throw new InvalidRequestException("You have already voted to skip this track");
        }

        // Step 4: Create vote record
        AppUser voter = appUserRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Vote vote = new Vote();
        vote.setRoom(room);
        vote.setVoter(voter);
        vote.setVoteType(VoteType.SKIPTRACK);
        vote.setPlaylistItemId(currentPlaylistItemId);
        vote.setCreatedAt(Instant.now());

        voteRepository.save(vote);
        log.info("Vote recorded: User {} voted to skip track {} in room {}", userId, currentPlaylistItemId, roomId);

        // Step 5: Count votes for this track
        long currentVoteCount = voteRepository.countByRoomIdAndPlaylistItemId(roomId, currentPlaylistItemId);

        // Step 6: Get online member count
        long onlineMemberCount = onlineMembersRedisService.getOnlineMemberCount(roomId.toString());

        // Step 7: Calculate required vote count (50% threshold)
        long requiredVoteCount = Math.max(1, (long) Math.ceil(onlineMemberCount * VOTE_THRESHOLD_PERCENTAGE));

        // Step 8: Check threshold
        boolean thresholdReached = currentVoteCount >= requiredVoteCount;

        VoteResponseDTO response = new VoteResponseDTO();
        response.setVoteRecorded(true);
        response.setCurrentVoteCount(currentVoteCount);
        response.setRequiredVoteCount(requiredVoteCount);
        response.setOnlineMemberCount(onlineMemberCount);
        response.setThresholdReached(thresholdReached);
        response.setTargetPlaylistItemId(currentPlaylistItemId);

        if (thresholdReached) {
            // Execute skip
            log.info(
                "Skip threshold reached for track {} in room {}: {} / {} votes",
                currentPlaylistItemId,
                roomId,
                currentVoteCount,
                requiredVoteCount
            );

            TrackOperationResult skipResult = playbackRedisService.skipTrack(roomId.toString());

            if (skipResult.isSuccess()) {
                response.setMessage("Track skipped successfully. Threshold reached (" + currentVoteCount + "/" + requiredVoteCount + ")");
                log.info("Track {} skipped successfully in room {}", currentPlaylistItemId, roomId);

                // Clean up old votes for the skipped track
                voteRepository.deleteSkipTrackVotesByPlaylistItemId(roomId, currentPlaylistItemId);
            } else {
                response.setMessage(
                    "Threshold reached but skip failed: " +
                    skipResult.getMessage() +
                    " (" +
                    currentVoteCount +
                    "/" +
                    requiredVoteCount +
                    ")"
                );
                log.error("Failed to skip track {} in room {}: {}", currentPlaylistItemId, roomId, skipResult.getMessage());
            }
            // TODO: Emit WebSocket event TRACK_SKIPPED to all room members
        } else {
            response.setMessage("Vote recorded. " + currentVoteCount + "/" + requiredVoteCount + " votes to skip");
            log.debug(
                "Skip threshold not reached for track {} in room {}: {} / {} votes",
                currentPlaylistItemId,
                roomId,
                currentVoteCount,
                requiredVoteCount
            );
            // TODO: Emit WebSocket event VOTE_CAST to all room members
        }

        return response;
    }

    /**
     * Vote to kick a user from the room.
     * Based on PROJECT_OVERVIEW.md section 2.9 - Kicking Users (Vote-Based).
     *
     * Workflow:
     * 1. Validate user is a room member
     * 2. Validate target user is a room member
     * 3. Validate user is not voting to kick themselves
     * 4. Validate target user is not the room owner
     * 5. Check if user already voted to kick this target
     * 6. Create vote record
     * 7. Count votes for this target
     * 8. Get online member count
     * 9. Check threshold (50%)
     * 10. If threshold reached, kick user (soft delete RoomMember, remove from Redis, close WebSocket)
     *
     * @param roomId Room UUID
     * @param userId User UUID (from JWT)
     * @param request KickUserRequestDTO with target user ID
     * @return VoteResponseDTO with vote status and whether threshold was reached
     * @throws ResourceNotFoundException if room or target user doesn't exist
     * @throws ForbiddenException if user is not a room member
     * @throws InvalidRequestException if validation fails (self-kick, owner kick, already voted)
     */
    public VoteResponseDTO voteKickUser(UUID roomId, UUID userId, KickUserRequestDTO request) {
        UUID targetUserId = request.getTargetUserId();
        log.debug("Vote to kick user {} in room {} by user {}", targetUserId, roomId, userId);

        // Step 1: Validate room exists
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // Step 1: Validate user is a room member
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this room");
        }

        // Step 2: Validate target user is a room member
        RoomMember targetMember = roomMemberRepository
            .findByRoomIdAndUserIdAndIsActiveTrue(roomId, targetUserId)
            .orElseThrow(() -> new InvalidRequestException("Target user is not a member of this room"));

        // Step 3: Validate user is not voting to kick themselves
        if (userId.equals(targetUserId)) {
            throw new InvalidRequestException("You cannot vote to kick yourself");
        }

        // Step 4: Validate target user is not the room owner
        boolean isOwner = roomMemberRepository.isRoomOwner(roomId, targetUserId);
        if (isOwner) {
            throw new InvalidRequestException("Cannot kick the room owner");
        }

        // Step 5: Check if user already voted to kick this target
        boolean alreadyVoted = voteRepository.existsByRoomIdAndVoterIdAndTargetUserId(roomId, userId, targetUserId);
        if (alreadyVoted) {
            throw new InvalidRequestException("You have already voted to kick this user");
        }

        // Step 6: Create vote record
        AppUser voter = appUserRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        AppUser targetUser = appUserRepository
            .findById(targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Target user not found: " + targetUserId));

        Vote vote = new Vote();
        vote.setRoom(room);
        vote.setVoter(voter);
        vote.setVoteType(VoteType.KICKUSER);
        vote.setTargetUser(targetUser);
        vote.setCreatedAt(Instant.now());

        voteRepository.save(vote);
        log.info("Vote recorded: User {} voted to kick user {} in room {}", userId, targetUserId, roomId);

        // Step 7: Count votes for this target
        long currentVoteCount = voteRepository.countByRoomIdAndTargetUserId(roomId, targetUserId);

        // Step 8: Get online member count
        long onlineMemberCount = onlineMembersRedisService.getOnlineMemberCount(roomId.toString());

        // Step 9: Calculate required vote count (50% threshold)
        long requiredVoteCount = Math.max(1, (long) Math.ceil(onlineMemberCount * VOTE_THRESHOLD_PERCENTAGE));

        // Step 10: Check threshold
        boolean thresholdReached = currentVoteCount >= requiredVoteCount;

        VoteResponseDTO response = new VoteResponseDTO();
        response.setVoteRecorded(true);
        response.setCurrentVoteCount(currentVoteCount);
        response.setRequiredVoteCount(requiredVoteCount);
        response.setOnlineMemberCount(onlineMemberCount);
        response.setThresholdReached(thresholdReached);
        response.setTargetUserId(targetUserId.toString());

        if (thresholdReached) {
            // Execute kick
            log.info(
                "Kick threshold reached for user {} in room {}: {} / {} votes",
                targetUserId,
                roomId,
                currentVoteCount,
                requiredVoteCount
            );

            // Soft delete: set isActive = false
            targetMember.setIsActive(false);
            targetMember.setLastActiveAt(Instant.now());
            roomMemberRepository.save(targetMember);

            // Remove from Redis online members
            onlineMembersRedisService.removeOnlineMember(roomId.toString(), targetUserId.toString());

            response.setMessage("User kicked successfully. Threshold reached (" + currentVoteCount + "/" + requiredVoteCount + ")");
            log.info("User {} kicked from room {}", targetUserId, roomId);

            // Clean up votes for this kicked user
            voteRepository.deleteKickUserVotesByTargetUserId(roomId, targetUserId);
            // TODO: Close WebSocket connection for kicked user
            // TODO: Emit WebSocket event USER_KICKED to all room members
        } else {
            response.setMessage("Vote recorded. " + currentVoteCount + "/" + requiredVoteCount + " votes to kick");
            log.debug(
                "Kick threshold not reached for user {} in room {}: {} / {} votes",
                targetUserId,
                roomId,
                currentVoteCount,
                requiredVoteCount
            );
            // TODO: Emit WebSocket event VOTE_CAST to all room members
        }

        return response;
    }

    /**
     * Get current vote status for a room.
     * Based on PROJECT_OVERVIEW.md sections 2.8 and 2.9.
     *
     * Returns:
     * - Current skip track vote status (if a track is playing)
     * - List of kick user vote statuses (for all users being voted to kick)
     *
     * @param roomId Room UUID
     * @param userId User UUID (from JWT) - for membership validation
     * @return VoteStatusResponseDTO with vote counts
     * @throws ResourceNotFoundException if room doesn't exist
     * @throws ForbiddenException if user is not a room member
     */
    @Transactional(readOnly = true)
    public VoteStatusResponseDTO getVoteStatus(UUID roomId, UUID userId) {
        log.debug("Get vote status for room {} by user {}", roomId, userId);

        // Validate room exists
        roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // Validate user is a room member
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this room");
        }

        VoteStatusResponseDTO response = new VoteStatusResponseDTO();

        // Get online member count (used for threshold calculation)
        long onlineMemberCount = onlineMembersRedisService.getOnlineMemberCount(roomId.toString());
        long requiredVoteCount = Math.max(1, (long) Math.ceil(onlineMemberCount * VOTE_THRESHOLD_PERCENTAGE));

        // Get skip track vote status
        String currentPlaylistItemId = playbackRedisService.getCurrentPlaylistItemId(roomId.toString());
        if (currentPlaylistItemId != null) {
            // There's a track currently playing
            long skipVoteCount = voteRepository.countByRoomIdAndPlaylistItemId(roomId, currentPlaylistItemId);

            // Get track metadata from Redis
            Map<Object, Object> playlistItem = playlistRedisService.getPlaylistItem(roomId.toString(), currentPlaylistItemId);

            String trackName = playlistItem.get("name") != null ? playlistItem.get("name").toString() : "Unknown Track";
            String artistName = playlistItem.get("artist") != null ? playlistItem.get("artist").toString() : "Unknown Artist";

            VoteStatusResponseDTO.SkipTrackVoteStatus skipStatus = new VoteStatusResponseDTO.SkipTrackVoteStatus(
                currentPlaylistItemId,
                trackName,
                artistName,
                skipVoteCount,
                requiredVoteCount,
                onlineMemberCount
            );

            response.setSkipTrackVote(skipStatus);
        }

        // Get kick user vote statuses
        List<Vote> kickVotes = voteRepository.findAllKickUserVotesByRoomId(roomId);

        // Group votes by target user and count
        Map<UUID, KickUserVoteInfo> kickVoteMap = new HashMap<>();
        for (Vote vote : kickVotes) {
            UUID targetUserId = vote.getTargetUser().getId();
            kickVoteMap.putIfAbsent(targetUserId, new KickUserVoteInfo(targetUserId, vote.getTargetUser().getDisplayName()));
            kickVoteMap.get(targetUserId).incrementCount();
        }

        // Convert to DTO list
        List<VoteStatusResponseDTO.KickUserVoteStatus> kickUserVoteStatuses = new ArrayList<>();
        for (KickUserVoteInfo info : kickVoteMap.values()) {
            VoteStatusResponseDTO.KickUserVoteStatus kickStatus = new VoteStatusResponseDTO.KickUserVoteStatus(
                info.targetUserId.toString(),
                info.displayName,
                info.voteCount,
                requiredVoteCount,
                onlineMemberCount
            );
            kickUserVoteStatuses.add(kickStatus);
        }

        response.setKickUserVotes(kickUserVoteStatuses);

        log.debug("Vote status retrieved for room {}: {} kick votes", roomId, kickUserVoteStatuses.size());
        return response;
    }

    /**
     * Withdraw a vote to skip the currently playing track.
     * Allows user to change their mind and remove their skip vote.
     *
     * Workflow:
     * 1. Validate user is a room member
     * 2. Check there is a track currently playing
     * 3. Check if user has voted for this track
     * 4. Delete the vote record
     * 5. Return updated vote counts
     *
     * @param roomId Room UUID
     * @param userId User UUID (from JWT)
     * @return VoteResponseDTO with updated vote status
     * @throws ResourceNotFoundException if room doesn't exist
     * @throws ForbiddenException if user is not a room member
     * @throws InvalidRequestException if no track is playing or user hasn't voted
     */
    public VoteResponseDTO withdrawSkipVote(UUID roomId, UUID userId) {
        log.debug("Withdraw skip vote in room {} by user {}", roomId, userId);

        // Step 1: Validate room exists
        roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // Step 1: Validate user is a room member
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this room");
        }

        // Step 2: Check there is a track currently playing
        String currentPlaylistItemId = playbackRedisService.getCurrentPlaylistItemId(roomId.toString());
        if (currentPlaylistItemId == null) {
            throw new InvalidRequestException("No track is currently playing");
        }

        // Step 3: Check if user has voted for this track
        boolean hasVoted = voteRepository.existsByRoomIdAndVoterIdAndPlaylistItemId(roomId, userId, currentPlaylistItemId);
        if (!hasVoted) {
            throw new InvalidRequestException("You have not voted to skip this track");
        }

        // Step 4: Delete the vote record
        int deletedCount = voteRepository.deleteSkipTrackVoteByVoter(roomId, userId, currentPlaylistItemId);

        if (deletedCount == 0) {
            log.warn("Failed to withdraw skip vote for user {} in room {} - vote not found", userId, roomId);
            throw new InvalidRequestException("Failed to withdraw vote - vote not found");
        }

        log.info("Vote withdrawn: User {} withdrew skip vote for track {} in room {}", userId, currentPlaylistItemId, roomId);

        // Step 5: Get updated vote counts
        long currentVoteCount = voteRepository.countByRoomIdAndPlaylistItemId(roomId, currentPlaylistItemId);
        long onlineMemberCount = onlineMembersRedisService.getOnlineMemberCount(roomId.toString());
        long requiredVoteCount = Math.max(1, (long) Math.ceil(onlineMemberCount * VOTE_THRESHOLD_PERCENTAGE));

        VoteResponseDTO response = new VoteResponseDTO();
        response.setVoteRecorded(false); // Vote was removed, not recorded
        response.setCurrentVoteCount(currentVoteCount);
        response.setRequiredVoteCount(requiredVoteCount);
        response.setOnlineMemberCount(onlineMemberCount);
        response.setThresholdReached(false);
        response.setTargetPlaylistItemId(currentPlaylistItemId);
        response.setMessage("Vote withdrawn successfully. " + currentVoteCount + "/" + requiredVoteCount + " votes to skip");

        // TODO: Emit WebSocket event VOTE_WITHDRAWN to all room members

        return response;
    }

    /**
     * Withdraw a vote to kick a user from the room.
     * Allows user to change their mind and remove their kick vote.
     *
     * Workflow:
     * 1. Validate user is a room member
     * 2. Validate target user is still a room member
     * 3. Check if user has voted to kick this target
     * 4. Delete the vote record
     * 5. Return updated vote counts
     *
     * @param roomId Room UUID
     * @param userId User UUID (from JWT)
     * @param request KickUserRequestDTO with target user ID
     * @return VoteResponseDTO with updated vote status
     * @throws ResourceNotFoundException if room or target user doesn't exist
     * @throws ForbiddenException if user is not a room member
     * @throws InvalidRequestException if user hasn't voted to kick this target
     */
    public VoteResponseDTO withdrawKickVote(UUID roomId, UUID userId, KickUserRequestDTO request) {
        UUID targetUserId = request.getTargetUserId();
        log.debug("Withdraw kick vote for user {} in room {} by user {}", targetUserId, roomId, userId);

        // Step 1: Validate room exists
        roomRepository.findById(roomId).orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        // Step 1: Validate user is a room member
        boolean isMember = roomMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this room");
        }

        // Step 2: Validate target user exists (they might have already left)
        appUserRepository.findById(targetUserId).orElseThrow(() -> new ResourceNotFoundException("Target user not found: " + targetUserId));

        // Step 3: Check if user has voted to kick this target
        boolean hasVoted = voteRepository.existsByRoomIdAndVoterIdAndTargetUserId(roomId, userId, targetUserId);
        if (!hasVoted) {
            throw new InvalidRequestException("You have not voted to kick this user");
        }

        // Step 4: Delete the vote record
        int deletedCount = voteRepository.deleteKickUserVoteByVoter(roomId, userId, targetUserId);

        if (deletedCount == 0) {
            log.warn("Failed to withdraw kick vote for user {} targeting {} in room {} - vote not found", userId, targetUserId, roomId);
            throw new InvalidRequestException("Failed to withdraw vote - vote not found");
        }

        log.info("Vote withdrawn: User {} withdrew kick vote for user {} in room {}", userId, targetUserId, roomId);

        // Step 5: Get updated vote counts
        long currentVoteCount = voteRepository.countByRoomIdAndTargetUserId(roomId, targetUserId);
        long onlineMemberCount = onlineMembersRedisService.getOnlineMemberCount(roomId.toString());
        long requiredVoteCount = Math.max(1, (long) Math.ceil(onlineMemberCount * VOTE_THRESHOLD_PERCENTAGE));

        VoteResponseDTO response = new VoteResponseDTO();
        response.setVoteRecorded(false); // Vote was removed, not recorded
        response.setCurrentVoteCount(currentVoteCount);
        response.setRequiredVoteCount(requiredVoteCount);
        response.setOnlineMemberCount(onlineMemberCount);
        response.setThresholdReached(false);
        response.setTargetUserId(targetUserId.toString());
        response.setMessage("Vote withdrawn successfully. " + currentVoteCount + "/" + requiredVoteCount + " votes to kick");

        // TODO: Emit WebSocket event VOTE_WITHDRAWN to all room members

        return response;
    }

    /**
     * Helper class for grouping kick votes by target user.
     */
    private static class KickUserVoteInfo {

        UUID targetUserId;
        String displayName;
        long voteCount;

        KickUserVoteInfo(UUID targetUserId, String displayName) {
            this.targetUserId = targetUserId;
            this.displayName = displayName;
            this.voteCount = 0;
        }

        void incrementCount() {
            this.voteCount++;
        }
    }
}
