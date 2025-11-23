package com.partywave.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents a PartyWave room.
 */
@Entity
@Table(name = "room")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @NotNull
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "room")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "appUser" }, allowSetters = true)
    private Set<RoomMember> members = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "room")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "appUser", "grantedBy" }, allowSetters = true)
    private Set<RoomAccess> accesses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "room")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "createdBy" }, allowSetters = true)
    private Set<RoomInvitation> invitations = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "room")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "sender" }, allowSetters = true)
    private Set<ChatMessage> messages = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "room")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "room", "voter", "targetUser" }, allowSetters = true)
    private Set<Vote> votes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "rel_room__tags", joinColumns = @JoinColumn(name = "room_id"), inverseJoinColumns = @JoinColumn(name = "tags_id"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "rooms" }, allowSetters = true)
    private Set<Tag> tags = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public UUID getId() {
        return this.id;
    }

    public Room id(UUID id) {
        this.setId(id);
        return this;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Room name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public Room description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMaxParticipants() {
        return this.maxParticipants;
    }

    public Room maxParticipants(Integer maxParticipants) {
        this.setMaxParticipants(maxParticipants);
        return this;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Boolean getIsPublic() {
        return this.isPublic;
    }

    public Room isPublic(Boolean isPublic) {
        this.setIsPublic(isPublic);
        return this;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Room createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Room updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<RoomMember> getMembers() {
        return this.members;
    }

    public void setMembers(Set<RoomMember> roomMembers) {
        if (this.members != null) {
            this.members.forEach(i -> i.setRoom(null));
        }
        if (roomMembers != null) {
            roomMembers.forEach(i -> i.setRoom(this));
        }
        this.members = roomMembers;
    }

    public Room members(Set<RoomMember> roomMembers) {
        this.setMembers(roomMembers);
        return this;
    }

    public Room addMembers(RoomMember roomMember) {
        this.members.add(roomMember);
        roomMember.setRoom(this);
        return this;
    }

    public Room removeMembers(RoomMember roomMember) {
        this.members.remove(roomMember);
        roomMember.setRoom(null);
        return this;
    }

    public Set<RoomAccess> getAccesses() {
        return this.accesses;
    }

    public void setAccesses(Set<RoomAccess> roomAccesses) {
        if (this.accesses != null) {
            this.accesses.forEach(i -> i.setRoom(null));
        }
        if (roomAccesses != null) {
            roomAccesses.forEach(i -> i.setRoom(this));
        }
        this.accesses = roomAccesses;
    }

    public Room accesses(Set<RoomAccess> roomAccesses) {
        this.setAccesses(roomAccesses);
        return this;
    }

    public Room addAccesses(RoomAccess roomAccess) {
        this.accesses.add(roomAccess);
        roomAccess.setRoom(this);
        return this;
    }

    public Room removeAccesses(RoomAccess roomAccess) {
        this.accesses.remove(roomAccess);
        roomAccess.setRoom(null);
        return this;
    }

    public Set<RoomInvitation> getInvitations() {
        return this.invitations;
    }

    public void setInvitations(Set<RoomInvitation> roomInvitations) {
        if (this.invitations != null) {
            this.invitations.forEach(i -> i.setRoom(null));
        }
        if (roomInvitations != null) {
            roomInvitations.forEach(i -> i.setRoom(this));
        }
        this.invitations = roomInvitations;
    }

    public Room invitations(Set<RoomInvitation> roomInvitations) {
        this.setInvitations(roomInvitations);
        return this;
    }

    public Room addInvitations(RoomInvitation roomInvitation) {
        this.invitations.add(roomInvitation);
        roomInvitation.setRoom(this);
        return this;
    }

    public Room removeInvitations(RoomInvitation roomInvitation) {
        this.invitations.remove(roomInvitation);
        roomInvitation.setRoom(null);
        return this;
    }

    public Set<ChatMessage> getMessages() {
        return this.messages;
    }

    public void setMessages(Set<ChatMessage> chatMessages) {
        if (this.messages != null) {
            this.messages.forEach(i -> i.setRoom(null));
        }
        if (chatMessages != null) {
            chatMessages.forEach(i -> i.setRoom(this));
        }
        this.messages = chatMessages;
    }

    public Room messages(Set<ChatMessage> chatMessages) {
        this.setMessages(chatMessages);
        return this;
    }

    public Room addMessages(ChatMessage chatMessage) {
        this.messages.add(chatMessage);
        chatMessage.setRoom(this);
        return this;
    }

    public Room removeMessages(ChatMessage chatMessage) {
        this.messages.remove(chatMessage);
        chatMessage.setRoom(null);
        return this;
    }

    public Set<Vote> getVotes() {
        return this.votes;
    }

    public void setVotes(Set<Vote> votes) {
        if (this.votes != null) {
            this.votes.forEach(i -> i.setRoom(null));
        }
        if (votes != null) {
            votes.forEach(i -> i.setRoom(this));
        }
        this.votes = votes;
    }

    public Room votes(Set<Vote> votes) {
        this.setVotes(votes);
        return this;
    }

    public Room addVotes(Vote vote) {
        this.votes.add(vote);
        vote.setRoom(this);
        return this;
    }

    public Room removeVotes(Vote vote) {
        this.votes.remove(vote);
        vote.setRoom(null);
        return this;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Room tags(Set<Tag> tags) {
        this.setTags(tags);
        return this;
    }

    public Room addTags(Tag tag) {
        this.tags.add(tag);
        return this;
    }

    public Room removeTags(Tag tag) {
        this.tags.remove(tag);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Room)) {
            return false;
        }
        return getId() != null && getId().equals(((Room) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Room{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", maxParticipants=" + getMaxParticipants() +
            ", isPublic='" + getIsPublic() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
