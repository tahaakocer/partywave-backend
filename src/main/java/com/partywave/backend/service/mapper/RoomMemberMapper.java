package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.RoomMember;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.RoomDTO;
import com.partywave.backend.service.dto.RoomMemberDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RoomMember} and its DTO {@link RoomMemberDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomMemberMapper extends EntityMapper<RoomMemberDTO, RoomMember> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomName")
    @Mapping(target = "appUser", source = "appUser", qualifiedByName = "appUserDisplayName")
    RoomMemberDTO toDto(RoomMember s);

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
