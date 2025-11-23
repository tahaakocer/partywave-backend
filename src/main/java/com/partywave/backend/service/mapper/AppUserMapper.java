package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.AppUserStats;
import com.partywave.backend.service.dto.AppUserDTO;
import com.partywave.backend.service.dto.AppUserStatsDTO;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AppUser} and its DTO {@link AppUserDTO}.
 */
@Mapper(componentModel = "spring")
public interface AppUserMapper extends EntityMapper<AppUserDTO, AppUser> {
    @Mapping(target = "stats", source = "stats", qualifiedByName = "appUserStatsId")
    AppUserDTO toDto(AppUser s);

    @Named("appUserStatsId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    AppUserStatsDTO toDtoAppUserStatsId(AppUserStats appUserStats);

    default String map(UUID value) {
        return Objects.toString(value, null);
    }
}
