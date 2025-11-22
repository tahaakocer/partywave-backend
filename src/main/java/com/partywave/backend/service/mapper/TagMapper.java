package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.Tag;
import com.partywave.backend.service.dto.RoomDTO;
import com.partywave.backend.service.dto.TagDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Tag} and its DTO {@link TagDTO}.
 */
@Mapper(componentModel = "spring")
public interface TagMapper extends EntityMapper<TagDTO, Tag> {
    @Mapping(target = "rooms", source = "rooms", qualifiedByName = "roomIdSet")
    TagDTO toDto(Tag s);

    @Mapping(target = "rooms", ignore = true)
    @Mapping(target = "removeRooms", ignore = true)
    Tag toEntity(TagDTO tagDTO);

    @Named("roomId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    RoomDTO toDtoRoomId(Room room);

    @Named("roomIdSet")
    default Set<RoomDTO> toDtoRoomIdSet(Set<Room> room) {
        return room.stream().map(this::toDtoRoomId).collect(Collectors.toSet());
    }
}
