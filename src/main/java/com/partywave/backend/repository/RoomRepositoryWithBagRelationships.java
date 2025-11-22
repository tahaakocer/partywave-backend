package com.partywave.backend.repository;

import com.partywave.backend.domain.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface RoomRepositoryWithBagRelationships {
    Optional<Room> fetchBagRelationships(Optional<Room> room);

    List<Room> fetchBagRelationships(List<Room> rooms);

    Page<Room> fetchBagRelationships(Page<Room> rooms);
}
