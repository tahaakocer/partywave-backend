package com.partywave.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.partywave.backend.domain.enumeration.AppUserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a PartyWave user linked to a Spotify account.
 */
@Entity
@Table(name = "app_user")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AppUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "spotify_user_id", nullable = false, unique = true)
    private String spotifyUserId;

    @NotNull
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "country")
    private String country;

    @Column(name = "href")
    private String href;

    @Column(name = "url")
    private String url;

    @Column(name = "type")
    private String type;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AppUserStatus status;

    @JsonIgnoreProperties(value = { "appUser" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private AppUserStats appUserStats;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "appUser")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "appUser" }, allowSetters = true)
    private Set<AppUserImage> images = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "appUser")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "appUser" }, allowSetters = true)
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "appUser")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "appUser" }, allowSetters = true)
    private Set<RoomMember> memberships = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "appUser")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "appUser", "grantedBy" }, allowSetters = true)
    private Set<RoomAccess> receivedAccesses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "grantedBy")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "appUser", "grantedBy" }, allowSetters = true)
    private Set<RoomAccess> grantedAccesses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "createdBy")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "createdBy" }, allowSetters = true)
    private Set<RoomInvitation> createdInvitations = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sender")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "sender" }, allowSetters = true)
    private Set<ChatMessage> messages = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "voter")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "voter", "targetUser" }, allowSetters = true)
    private Set<Vote> castVotes = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "targetUser")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "voter", "targetUser" }, allowSetters = true)
    private Set<Vote> receivedVotes = new HashSet<>();

    @JsonIgnoreProperties(value = { "appUser" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "appUser")
    private UserToken userToken;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public AppUser id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpotifyUserId() {
        return this.spotifyUserId;
    }

    public AppUser spotifyUserId(String spotifyUserId) {
        this.setSpotifyUserId(spotifyUserId);
        return this;
    }

    public void setSpotifyUserId(String spotifyUserId) {
        this.spotifyUserId = spotifyUserId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public AppUser displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return this.email;
    }

    public AppUser email(String email) {
        this.setEmail(email);
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountry() {
        return this.country;
    }

    public AppUser country(String country) {
        this.setCountry(country);
        return this;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getHref() {
        return this.href;
    }

    public AppUser href(String href) {
        this.setHref(href);
        return this;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getUrl() {
        return this.url;
    }

    public AppUser url(String url) {
        this.setUrl(url);
        return this;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return this.type;
    }

    public AppUser type(String type) {
        this.setType(type);
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public AppUser ipAddress(String ipAddress) {
        this.setIpAddress(ipAddress);
        return this;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Instant getLastActiveAt() {
        return this.lastActiveAt;
    }

    public AppUser lastActiveAt(Instant lastActiveAt) {
        this.setLastActiveAt(lastActiveAt);
        return this;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public AppUserStatus getStatus() {
        return this.status;
    }

    public AppUser status(AppUserStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(AppUserStatus status) {
        this.status = status;
    }

    public AppUserStats getAppUserStats() {
        return this.appUserStats;
    }

    public void setAppUserStats(AppUserStats appUserStats) {
        this.appUserStats = appUserStats;
    }

    public AppUser appUserStats(AppUserStats appUserStats) {
        this.setAppUserStats(appUserStats);
        return this;
    }

    public Set<AppUserImage> getImages() {
        return this.images;
    }

    public void setImages(Set<AppUserImage> appUserImages) {
        if (this.images != null) {
            this.images.forEach(i -> i.setAppUser(null));
        }
        if (appUserImages != null) {
            appUserImages.forEach(i -> i.setAppUser(this));
        }
        this.images = appUserImages;
    }

    public AppUser images(Set<AppUserImage> appUserImages) {
        this.setImages(appUserImages);
        return this;
    }

    public AppUser addImages(AppUserImage appUserImage) {
        this.images.add(appUserImage);
        appUserImage.setAppUser(this);
        return this;
    }

    public AppUser removeImages(AppUserImage appUserImage) {
        this.images.remove(appUserImage);
        appUserImage.setAppUser(null);
        return this;
    }

    public Set<RefreshToken> getRefreshTokens() {
        return this.refreshTokens;
    }

    public void setRefreshTokens(Set<RefreshToken> refreshTokens) {
        if (this.refreshTokens != null) {
            this.refreshTokens.forEach(i -> i.setAppUser(null));
        }
        if (refreshTokens != null) {
            refreshTokens.forEach(i -> i.setAppUser(this));
        }
        this.refreshTokens = refreshTokens;
    }

    public AppUser refreshTokens(Set<RefreshToken> refreshTokens) {
        this.setRefreshTokens(refreshTokens);
        return this;
    }

    public AppUser addRefreshTokens(RefreshToken refreshToken) {
        this.refreshTokens.add(refreshToken);
        refreshToken.setAppUser(this);
        return this;
    }

    public AppUser removeRefreshTokens(RefreshToken refreshToken) {
        this.refreshTokens.remove(refreshToken);
        refreshToken.setAppUser(null);
        return this;
    }

    public Set<RoomMember> getMemberships() {
        return this.memberships;
    }

    public void setMemberships(Set<RoomMember> roomMembers) {
        if (this.memberships != null) {
            this.memberships.forEach(i -> i.setAppUser(null));
        }
        if (roomMembers != null) {
            roomMembers.forEach(i -> i.setAppUser(this));
        }
        this.memberships = roomMembers;
    }

    public AppUser memberships(Set<RoomMember> roomMembers) {
        this.setMemberships(roomMembers);
        return this;
    }

    public AppUser addMemberships(RoomMember roomMember) {
        this.memberships.add(roomMember);
        roomMember.setAppUser(this);
        return this;
    }

    public AppUser removeMemberships(RoomMember roomMember) {
        this.memberships.remove(roomMember);
        roomMember.setAppUser(null);
        return this;
    }

    public Set<RoomAccess> getReceivedAccesses() {
        return this.receivedAccesses;
    }

    public void setReceivedAccesses(Set<RoomAccess> roomAccesses) {
        if (this.receivedAccesses != null) {
            this.receivedAccesses.forEach(i -> i.setAppUser(null));
        }
        if (roomAccesses != null) {
            roomAccesses.forEach(i -> i.setAppUser(this));
        }
        this.receivedAccesses = roomAccesses;
    }

    public AppUser receivedAccesses(Set<RoomAccess> roomAccesses) {
        this.setReceivedAccesses(roomAccesses);
        return this;
    }

    public AppUser addReceivedAccesses(RoomAccess roomAccess) {
        this.receivedAccesses.add(roomAccess);
        roomAccess.setAppUser(this);
        return this;
    }

    public AppUser removeReceivedAccesses(RoomAccess roomAccess) {
        this.receivedAccesses.remove(roomAccess);
        roomAccess.setAppUser(null);
        return this;
    }

    public Set<RoomAccess> getGrantedAccesses() {
        return this.grantedAccesses;
    }

    public void setGrantedAccesses(Set<RoomAccess> roomAccesses) {
        if (this.grantedAccesses != null) {
            this.grantedAccesses.forEach(i -> i.setGrantedBy(null));
        }
        if (roomAccesses != null) {
            roomAccesses.forEach(i -> i.setGrantedBy(this));
        }
        this.grantedAccesses = roomAccesses;
    }

    public AppUser grantedAccesses(Set<RoomAccess> roomAccesses) {
        this.setGrantedAccesses(roomAccesses);
        return this;
    }

    public AppUser addGrantedAccesses(RoomAccess roomAccess) {
        this.grantedAccesses.add(roomAccess);
        roomAccess.setGrantedBy(this);
        return this;
    }

    public AppUser removeGrantedAccesses(RoomAccess roomAccess) {
        this.grantedAccesses.remove(roomAccess);
        roomAccess.setGrantedBy(null);
        return this;
    }

    public Set<RoomInvitation> getCreatedInvitations() {
        return this.createdInvitations;
    }

    public void setCreatedInvitations(Set<RoomInvitation> roomInvitations) {
        if (this.createdInvitations != null) {
            this.createdInvitations.forEach(i -> i.setCreatedBy(null));
        }
        if (roomInvitations != null) {
            roomInvitations.forEach(i -> i.setCreatedBy(this));
        }
        this.createdInvitations = roomInvitations;
    }

    public AppUser createdInvitations(Set<RoomInvitation> roomInvitations) {
        this.setCreatedInvitations(roomInvitations);
        return this;
    }

    public AppUser addCreatedInvitations(RoomInvitation roomInvitation) {
        this.createdInvitations.add(roomInvitation);
        roomInvitation.setCreatedBy(this);
        return this;
    }

    public AppUser removeCreatedInvitations(RoomInvitation roomInvitation) {
        this.createdInvitations.remove(roomInvitation);
        roomInvitation.setCreatedBy(null);
        return this;
    }

    public Set<ChatMessage> getMessages() {
        return this.messages;
    }

    public void setMessages(Set<ChatMessage> chatMessages) {
        if (this.messages != null) {
            this.messages.forEach(i -> i.setSender(null));
        }
        if (chatMessages != null) {
            chatMessages.forEach(i -> i.setSender(this));
        }
        this.messages = chatMessages;
    }

    public AppUser messages(Set<ChatMessage> chatMessages) {
        this.setMessages(chatMessages);
        return this;
    }

    public AppUser addMessages(ChatMessage chatMessage) {
        this.messages.add(chatMessage);
        chatMessage.setSender(this);
        return this;
    }

    public AppUser removeMessages(ChatMessage chatMessage) {
        this.messages.remove(chatMessage);
        chatMessage.setSender(null);
        return this;
    }

    public Set<Vote> getCastVotes() {
        return this.castVotes;
    }

    public void setCastVotes(Set<Vote> votes) {
        if (this.castVotes != null) {
            this.castVotes.forEach(i -> i.setVoter(null));
        }
        if (votes != null) {
            votes.forEach(i -> i.setVoter(this));
        }
        this.castVotes = votes;
    }

    public AppUser castVotes(Set<Vote> votes) {
        this.setCastVotes(votes);
        return this;
    }

    public AppUser addCastVotes(Vote vote) {
        this.castVotes.add(vote);
        vote.setVoter(this);
        return this;
    }

    public AppUser removeCastVotes(Vote vote) {
        this.castVotes.remove(vote);
        vote.setVoter(null);
        return this;
    }

    public Set<Vote> getReceivedVotes() {
        return this.receivedVotes;
    }

    public void setReceivedVotes(Set<Vote> votes) {
        if (this.receivedVotes != null) {
            this.receivedVotes.forEach(i -> i.setTargetUser(null));
        }
        if (votes != null) {
            votes.forEach(i -> i.setTargetUser(this));
        }
        this.receivedVotes = votes;
    }

    public AppUser receivedVotes(Set<Vote> votes) {
        this.setReceivedVotes(votes);
        return this;
    }

    public AppUser addReceivedVotes(Vote vote) {
        this.receivedVotes.add(vote);
        vote.setTargetUser(this);
        return this;
    }

    public AppUser removeReceivedVotes(Vote vote) {
        this.receivedVotes.remove(vote);
        vote.setTargetUser(null);
        return this;
    }

    public UserToken getUserToken() {
        return this.userToken;
    }

    public void setUserToken(UserToken userToken) {
        if (this.userToken != null) {
            this.userToken.setAppUser(null);
        }
        if (userToken != null) {
            userToken.setAppUser(this);
        }
        this.userToken = userToken;
    }

    public AppUser userToken(UserToken userToken) {
        this.setUserToken(userToken);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppUser)) {
            return false;
        }
        return getId() != null && getId().equals(((AppUser) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "AppUser{" +
            "id=" + getId() +
            ", spotifyUserId='" + getSpotifyUserId() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", email='" + getEmail() + "'" +
            ", country='" + getCountry() + "'" +
            ", href='" + getHref() + "'" +
            ", url='" + getUrl() + "'" +
            ", type='" + getType() + "'" +
            ", ipAddress='" + getIpAddress() + "'" +
            ", lastActiveAt='" + getLastActiveAt() + "'" +
            ", status='" + getStatus() + "'" +
            "}";
    }
}
