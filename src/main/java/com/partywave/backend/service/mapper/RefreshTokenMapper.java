package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.RefreshToken;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.RefreshTokenDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link RefreshToken} and its DTO {@link RefreshTokenDTO}.
 */
@Mapper(componentModel = "spring")
public interface RefreshTokenMapper extends EntityMapper<RefreshTokenDTO, RefreshToken> {
    @Mapping(target = "appUser", source = "appUser", qualifiedByName = "appUserDisplayName")
    RefreshTokenDTO toDto(RefreshToken s);

    @Named("appUserDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    AppUserDTO toDtoAppUserDisplayName(AppUser appUser);
}
