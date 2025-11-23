package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * DTO for room creation requests.
 * Used by authenticated users to create new PartyWave rooms.
 *
 * Based on PROJECT_OVERVIEW.md section 2.2 - Room Creation.
 */
public class CreateRoomRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Room name is required")
    @Size(min = 1, max = 100, message = "Room name must be between 1 and 100 characters")
    @JsonProperty("name")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @JsonProperty("description")
    private String description;

    @JsonProperty("tags")
    private List<String> tags;

    @NotNull(message = "Max participants is required")
    @Min(value = 1, message = "Max participants must be at least 1")
    @JsonProperty("max_participants")
    private Integer maxParticipants;

    @NotNull(message = "Is public flag is required")
    @JsonProperty("is_public")
    private Boolean isPublic;

    public CreateRoomRequestDTO() {}

    public CreateRoomRequestDTO(String name, String description, List<String> tags, Integer maxParticipants, Boolean isPublic) {
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.maxParticipants = maxParticipants;
        this.isPublic = isPublic;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
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

    @Override
    public String toString() {
        return (
            "CreateRoomRequestDTO{" +
            "name='" +
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
            '}'
        );
    }
}
