package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.ChatMessage;
import com.partywave.backend.domain.Room;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.ChatMessageDTO;
import com.partywave.backend.service.dto.RoomDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link ChatMessage} and its DTO {@link ChatMessageDTO}.
 */
@Mapper(componentModel = "spring")
public interface ChatMessageMapper extends EntityMapper<ChatMessageDTO, ChatMessage> {
    @Mapping(target = "room", source = "room", qualifiedByName = "roomName")
    @Mapping(target = "sender", source = "sender", qualifiedByName = "appUserDisplayName")
    ChatMessageDTO toDto(ChatMessage s);

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
