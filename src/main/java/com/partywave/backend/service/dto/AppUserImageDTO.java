package com.partywave.backend.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.partywave.backend.domain.AppUserImage} entity.
 */
@Schema(description = "Profile images associated with an AppUser.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AppUserImageDTO implements Serializable {

    private Long id;

    @NotNull
    private String url;

    private String height;

    private String width;

    private AppUserDTO appUser;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public AppUserDTO getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUserDTO appUser) {
        this.appUser = appUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppUserImageDTO)) {
            return false;
        }

        AppUserImageDTO appUserImageDTO = (AppUserImageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, appUserImageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppUserImageDTO{" +
            "id=" + getId() +
            ", url='" + getUrl() + "'" +
            ", height='" + getHeight() + "'" +
            ", width='" + getWidth() + "'" +
            ", appUser=" + getAppUser() +
            "}";
    }
}
