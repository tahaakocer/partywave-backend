package com.partywave.backend.service.mapper;

import com.partywave.backend.domain.AppUserStats;
import com.partywave.backend.service.dto.AppUserStatsDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AppUserStats} and its DTO {@link AppUserStatsDTO}.
 */
@Mapper(componentModel = "spring")
public interface AppUserStatsMapper extends EntityMapper<AppUserStatsDTO, AppUserStats> {}
