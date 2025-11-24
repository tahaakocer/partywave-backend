package com.partywave.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * DTO for updating user profile.
 * Used for PUT /api/users/me endpoint.
 */
public class UpdateUserProfileRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("sync_images_from_spotify")
    private Boolean syncImagesFromSpotify;

    public UpdateUserProfileRequestDTO() {}

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getSyncImagesFromSpotify() {
        return syncImagesFromSpotify;
    }

    public void setSyncImagesFromSpotify(Boolean syncImagesFromSpotify) {
        this.syncImagesFromSpotify = syncImagesFromSpotify;
    }

    @Override
    public String toString() {
        return (
            "UpdateUserProfileRequestDTO{" + "displayName='" + displayName + '\'' + ", syncImagesFromSpotify=" + syncImagesFromSpotify + '}'
        );
    }
}
