package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.UserToken;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.UserTokenDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link UserToken} and its DTO {@link UserTokenDTO}.
 */
@Mapper(componentModel = "spring")
public interface UserTokenMapper extends EntityMapper<UserTokenDTO, UserToken> {
    @Mapping(target = "appUser", source = "appUser", qualifiedByName = "appUserDisplayName")
    UserTokenDTO toDto(UserToken s);

    @Named("appUserDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    AppUserDTO toDtoAppUserDisplayName(AppUser appUser);
}
