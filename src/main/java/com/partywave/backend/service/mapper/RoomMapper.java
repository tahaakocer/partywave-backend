package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.Tag;
import com.partywave.backend.service.dto.RoomDTO;
import com.partywave.backend.service.dto.TagDTO;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Room} and its DTO {@link RoomDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomMapper extends EntityMapper<RoomDTO, Room> {
    @Mapping(target = "tags", source = "tags", qualifiedByName = "tagNameSet")
    RoomDTO toDto(Room s);

    @Mapping(target = "removeTags", ignore = true)
    Room toEntity(RoomDTO roomDTO);

    @Named("tagName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    TagDTO toDtoTagName(Tag tag);

    @Named("tagNameSet")
    default Set<TagDTO> toDtoTagNameSet(Set<Tag> tag) {
        return tag.stream().map(this::toDtoTagName).collect(Collectors.toSet());
    }
}
