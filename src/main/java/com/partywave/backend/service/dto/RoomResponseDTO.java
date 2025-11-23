package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for room responses.
 * Used to return room data to clients after creation or retrieval.
 *
 * Based on PROJECT_OVERVIEW.md section 2.2 - Room Creation.
 */
public class RoomResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tags")
    private List<TagDTO> tags;

    @JsonProperty("max_participants")
    private Integer maxParticipants;

    @JsonProperty("is_public")
    private Boolean isPublic;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("member_count")
    private Integer memberCount;

    @JsonProperty("online_member_count")
    private Long onlineMemberCount;

    public RoomResponseDTO() {}

    public RoomResponseDTO(
        UUID id,
        String name,
        String description,
        List<TagDTO> tags,
        Integer maxParticipants,
        Boolean isPublic,
        Instant createdAt,
        Instant updatedAt,
        Integer memberCount,
        Long onlineMemberCount
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.maxParticipants = maxParticipants;
        this.isPublic = isPublic;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.memberCount = memberCount;
        this.onlineMemberCount = onlineMemberCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TagDTO> getTags() {
        return tags;
    }

    public void setTags(List<TagDTO> tags) {
        this.tags = tags;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public Long getOnlineMemberCount() {
        return onlineMemberCount;
    }

    public void setOnlineMemberCount(Long onlineMemberCount) {
        this.onlineMemberCount = onlineMemberCount;
    }

    @Override
    public String toString() {
        return (
            "RoomResponseDTO{" +
            "id=" +
            id +
            ", name='" +
            name +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", tags=" +
            tags +
            ", maxParticipants=" +
            maxParticipants +
            ", isPublic=" +
            isPublic +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            ", memberCount=" +
            memberCount +
            ", onlineMemberCount=" +
            onlineMemberCount +
            '}'
        );
    }
}
