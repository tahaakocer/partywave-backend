package com.partywave.backend.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the complete state of a room when a user joins.
 * Based on PROJECT_OVERVIEW.md section 2.3 - Room Joining Response.
 *
 * This includes:
 * - Room details (name, description, tags, max participants)
 * - Complete playlist with track metadata and feedback counts
 * - Current playback state (if a track is playing)
 * - Recent chat history
 * - Member counts (total and online)
 */
public class RoomStateResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private RoomResponseDTO room;
    private List<PlaylistItemDTO> playlist;
    private PlaybackStateDTO playbackState;
    private List<ChatMessageDTO> chatHistory;

    // Constructors
    public RoomStateResponseDTO() {
        this.playlist = new ArrayList<>();
        this.chatHistory = new ArrayList<>();
    }

    // Getters and Setters

    public RoomResponseDTO getRoom() {
        return room;
    }

    public void setRoom(RoomResponseDTO room) {
        this.room = room;
    }

    public List<PlaylistItemDTO> getPlaylist() {
        return playlist;
    }

    public void setPlaylist(List<PlaylistItemDTO> playlist) {
        this.playlist = playlist;
    }

    public PlaybackStateDTO getPlaybackState() {
        return playbackState;
    }

    public void setPlaybackState(PlaybackStateDTO playbackState) {
        this.playbackState = playbackState;
    }

    public List<ChatMessageDTO> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<ChatMessageDTO> chatHistory) {
        this.chatHistory = chatHistory;
    }

    @Override
    public String toString() {
        return (
            "RoomStateResponseDTO{" +
            "room=" +
            room +
            ", playlistSize=" +
            (playlist != null ? playlist.size() : 0) +
            ", hasPlaybackState=" +
            (playbackState != null) +
            ", chatHistorySize=" +
            (chatHistory != null ? chatHistory.size() : 0) +
            '}'
        );
    }
}
