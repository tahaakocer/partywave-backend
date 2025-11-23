package com.partywave.backend.repository;

import com.partywave.backend.domain.ChatMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ChatMessage entity.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    default Optional<ChatMessage> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ChatMessage> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ChatMessage> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select chatMessage from ChatMessage chatMessage left join fetch chatMessage.room left join fetch chatMessage.sender",
        countQuery = "select count(chatMessage) from ChatMessage chatMessage"
    )
    Page<ChatMessage> findAllWithToOneRelationships(Pageable pageable);

    @Query("select chatMessage from ChatMessage chatMessage left join fetch chatMessage.room left join fetch chatMessage.sender")
    List<ChatMessage> findAllWithToOneRelationships();

    @Query(
        "select chatMessage from ChatMessage chatMessage left join fetch chatMessage.room left join fetch chatMessage.sender where chatMessage.id =:id"
    )
    Optional<ChatMessage> findOneWithToOneRelationships(@Param("id") UUID id);

    /**
     * Find chat messages for a room, ordered by sentAt descending (newest first).
     * Used to fetch recent chat history when joining a room.
     *
     * @param roomId Room UUID
     * @param pageable Pagination parameters (typically Page 0, Size N for last N messages)
     * @return Page of chat messages
     */
    @Query(
        value = "select chatMessage from ChatMessage chatMessage left join fetch chatMessage.sender where chatMessage.room.id = :roomId order by chatMessage.sentAt desc",
        countQuery = "select count(chatMessage) from ChatMessage chatMessage where chatMessage.room.id = :roomId"
    )
    Page<ChatMessage> findByRoomIdOrderBySentAtDesc(@Param("roomId") UUID roomId, Pageable pageable);
}
