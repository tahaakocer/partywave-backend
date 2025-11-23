package com.partywave.backend.repository;

import com.partywave.backend.domain.Room;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class RoomRepositoryWithBagRelationshipsImpl implements RoomRepositoryWithBagRelationships {

    private static final String ID_PARAMETER = "id";
    private static final String ROOMS_PARAMETER = "rooms";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Room> fetchBagRelationships(Optional<Room> room) {
        return room.map(this::fetchTags);
    }

    @Override
    public Page<Room> fetchBagRelationships(Page<Room> rooms) {
        return new PageImpl<>(fetchBagRelationships(rooms.getContent()), rooms.getPageable(), rooms.getTotalElements());
    }

    @Override
    public List<Room> fetchBagRelationships(List<Room> rooms) {
        return Optional.of(rooms).map(this::fetchTags).orElse(Collections.emptyList());
    }

    Room fetchTags(Room result) {
        return entityManager
            .createQuery("select room from Room room left join fetch room.tags where room.id = :id", Room.class)
            .setParameter(ID_PARAMETER, result.getId())
            .getSingleResult();
    }

    List<Room> fetchTags(List<Room> rooms) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, rooms.size()).forEach(index -> order.put(rooms.get(index).getId(), index));
        List<Room> result = entityManager
            .createQuery("select room from Room room left join fetch room.tags where room in :rooms", Room.class)
            .setParameter(ROOMS_PARAMETER, rooms)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
