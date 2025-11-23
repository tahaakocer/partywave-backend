package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.AppUserImage;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.AppUserImageDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AppUserImage} and its DTO {@link AppUserImageDTO}.
 */
@Mapper(componentModel = "spring")
public interface AppUserImageMapper extends EntityMapper<AppUserImageDTO, AppUserImage> {
    @Mapping(target = "appUser", source = "appUser", qualifiedByName = "appUserDisplayName")
    AppUserImageDTO toDto(AppUserImage s);

    @Named("appUserDisplayName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "displayName", source = "displayName")
    AppUserDTO toDtoAppUserDisplayName(AppUser appUser);
}
