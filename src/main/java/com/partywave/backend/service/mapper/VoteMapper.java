package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.Vote;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.RoomDTO;
import com.partywave.backend.service.dto.VoteDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Vote} and its DTO {@link VoteDTO}.
 */
@Mapper(componentModel = "spring")
public interface VoteMapper extends EntityMapper<VoteDTO, Vote> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomName")
    @Mapping(target = "voter", source = "voter", qualifiedByName = "appUserDisplayName")
    @Mapping(target = "targetUser", source = "targetUser", qualifiedByName = "appUserDisplayName")
    VoteDTO toDto(Vote s);

    @Named("roomName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    RoomDTO toDtoRoomName(Room room);

    @Named("appUserDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    AppUserDTO toDtoAppUserDisplayName(AppUser appUser);
}
