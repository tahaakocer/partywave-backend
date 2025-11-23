package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.Room;
import com.partywave.backend.domain.RoomInvitation;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.RoomDTO;
import com.partywave.backend.service.dto.RoomInvitationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RoomInvitation} and its DTO {@link RoomInvitationDTO}.
 */
@Mapper(componentModel = "spring")
public interface RoomInvitationMapper extends EntityMapper<RoomInvitationDTO, RoomInvitation> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomName")
    @Mapping(target = "createdBy", source = "createdBy", qualifiedByName = "appUserDisplayName")
    RoomInvitationDTO toDto(RoomInvitation s);

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
