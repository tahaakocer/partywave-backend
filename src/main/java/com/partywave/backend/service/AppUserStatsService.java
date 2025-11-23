package com.partywave.backend.service;

import com.partywave.backend.domain.AppUserStats;
import com.partywave.backend.repository.AppUserStatsRepository;
import com.partywave.backend.service.dto.AppUserStatsDTO;
import com.partywave.backend.service.mapper.AppUserStatsMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.partywave.backend.domain.AppUserStats}.
 */
@Service
@Transactional
public class AppUserStatsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserStatsService.class);

    private final AppUserStatsRepository appUserStatsRepository;

    private final AppUserStatsMapper appUserStatsMapper;

    public AppUserStatsService(AppUserStatsRepository appUserStatsRepository, AppUserStatsMapper appUserStatsMapper) {
        this.appUserStatsRepository = appUserStatsRepository;
        this.appUserStatsMapper = appUserStatsMapper;
    }

    /**
     * Save a appUserStats.
     *
     * @param appUserStatsDTO the entity to save.
     * @return the persisted entity.
     */
    public AppUserStatsDTO save(AppUserStatsDTO appUserStatsDTO) {
        LOG.debug("Request to save AppUserStats : {}", appUserStatsDTO);
        AppUserStats appUserStats = appUserStatsMapper.toEntity(appUserStatsDTO);
        appUserStats = appUserStatsRepository.save(appUserStats);
        return appUserStatsMapper.toDto(appUserStats);
    }

    /**
     * Update a appUserStats.
     *
     * @param appUserStatsDTO the entity to save.
     * @return the persisted entity.
     */
    public AppUserStatsDTO update(AppUserStatsDTO appUserStatsDTO) {
        LOG.debug("Request to update AppUserStats : {}", appUserStatsDTO);
        AppUserStats appUserStats = appUserStatsMapper.toEntity(appUserStatsDTO);
        appUserStats = appUserStatsRepository.save(appUserStats);
        return appUserStatsMapper.toDto(appUserStats);
    }

    /**
     * Partially update a appUserStats.
     *
     * @param appUserStatsDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AppUserStatsDTO> partialUpdate(AppUserStatsDTO appUserStatsDTO) {
        LOG.debug("Request to partially update AppUserStats : {}", appUserStatsDTO);

        return appUserStatsRepository
            .findById(appUserStatsDTO.getId())
            .map(existingAppUserStats -> {
                appUserStatsMapper.partialUpdate(existingAppUserStats, appUserStatsDTO);

                return existingAppUserStats;
            })
            .map(appUserStatsRepository::save)
            .map(appUserStatsMapper::toDto);
    }

    /**
     * Get all the appUserStats.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AppUserStatsDTO> findAll() {
        LOG.debug("Request to get all AppUserStats");
        return appUserStatsRepository.findAll().stream().map(appUserStatsMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the appUserStats where AppUser is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AppUserStatsDTO> findAllWhereAppUserIsNull() {
        LOG.debug("Request to get all appUserStats where AppUser is null");
        return StreamSupport.stream(appUserStatsRepository.findAll().spliterator(), false)
            .filter(appUserStats -> appUserStats.getAppUser() == null)
            .map(appUserStatsMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one appUserStats by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AppUserStatsDTO> findOne(UUID id) {
        LOG.debug("Request to get AppUserStats : {}", id);
        return appUserStatsRepository.findById(id).map(appUserStatsMapper::toDto);
    }

    /**
     * Delete the appUserStats by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AppUserStats : {}", id);
        appUserStatsRepository.deleteById(id);
    }
}
