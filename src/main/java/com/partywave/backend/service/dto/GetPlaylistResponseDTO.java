package com.partywave.backend.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for GET /api/rooms/{roomId}/playlist response.
 * Contains the complete playlist (active + history tracks) sorted by sequence number.
 */
public class GetPlaylistResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String roomId;
    private List<PlaylistItemDTO> items;
    private Integer totalCount;

    // Constructors
    public GetPlaylistResponseDTO() {
        this.items = new ArrayList<>();
    }

    public GetPlaylistResponseDTO(String roomId, List<PlaylistItemDTO> items) {
        this.roomId = roomId;
        this.items = items != null ? items : new ArrayList<>();
        this.totalCount = this.items.size();
    }

    // Getters and Setters

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<PlaylistItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PlaylistItemDTO> items) {
        this.items = items;
        this.totalCount = items != null ? items.size() : 0;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public String toString() {
        return "GetPlaylistResponseDTO{" + "roomId='" + roomId + '\'' + ", totalCount=" + totalCount + '}';
    }
}
