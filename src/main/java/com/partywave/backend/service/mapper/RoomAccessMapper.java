package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.RoomAccess;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.RoomAccessDTO;
import com.partywave.backend.service.dto.RoomDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RoomAccess} and its DTO {@link RoomAccessDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomAccessMapper extends EntityMapper<RoomAccessDTO, RoomAccess> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomName")
    @Mapping(target = "appUser", source = "appUser", qualifiedByName = "appUserDisplayName")
    @Mapping(target = "grantedBy", source = "grantedBy", qualifiedByName = "appUserDisplayName")
    RoomAccessDTO toDto(RoomAccess s);

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
