package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.Room;
import com.partywave.backend.service.dto.RoomResponseDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Room} and its DTO {@link RoomResponseDTO}.
 *
 * Converts between Room entities and RoomResponseDTO for API responses.
 */
@Mapper(componentModel = "spring", uses = { TagMapper.class })
@SuppressWarnings("common-java:DuplicatedBlocks")
public interface RoomMapper extends EntityMapper<RoomResponseDTO, Room> {
    @Mapping(target = "memberCount", ignore = true) // Set manually in service
    @Mapping(target = "onlineMemberCount", ignore = true) // Set manually in service
    RoomResponseDTO toDto(Room room);

    @Mapping(target = "members", ignore = true)
    @Mapping(target = "removeMembers", ignore = true)
    @Mapping(target = "accesses", ignore = true)
    @Mapping(target = "removeAccesses", ignore = true)
    @Mapping(target = "invitations", ignore = true)
    @Mapping(target = "removeInvitations", ignore = true)
    @Mapping(target = "messages", ignore = true)
    @Mapping(target = "removeMessages", ignore = true)
    @Mapping(target = "votes", ignore = true)
    @Mapping(target = "removeVotes", ignore = true)
    @Mapping(target = "removeTags", ignore = true)
    Room toEntity(RoomResponseDTO roomResponseDTO);
}
