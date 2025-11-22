package com.partywave.backend.service;

import com.partywave.backend.domain.AppUserImage;
import com.partywave.backend.repository.AppUserImageRepository;
import com.partywave.backend.service.dto.AppUserImageDTO;
import com.partywave.backend.service.mapper.AppUserImageMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.partywave.backend.domain.AppUserImage}.
 */
@Service
@Transactional
public class AppUserImageService {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserImageService.class);

    private final AppUserImageRepository appUserImageRepository;

    private final AppUserImageMapper appUserImageMapper;

    public AppUserImageService(AppUserImageRepository appUserImageRepository, AppUserImageMapper appUserImageMapper) {
        this.appUserImageRepository = appUserImageRepository;
        this.appUserImageMapper = appUserImageMapper;
    }

    /**
     * Save a appUserImage.
     *
     * @param appUserImageDTO the entity to save.
     * @return the persisted entity.
     */
    public AppUserImageDTO save(AppUserImageDTO appUserImageDTO) {
        LOG.debug("Request to save AppUserImage : {}", appUserImageDTO);
        AppUserImage appUserImage = appUserImageMapper.toEntity(appUserImageDTO);
        appUserImage = appUserImageRepository.save(appUserImage);
        return appUserImageMapper.toDto(appUserImage);
    }

    /**
     * Update a appUserImage.
     *
     * @param appUserImageDTO the entity to save.
     * @return the persisted entity.
     */
    public AppUserImageDTO update(AppUserImageDTO appUserImageDTO) {
        LOG.debug("Request to update AppUserImage : {}", appUserImageDTO);
        AppUserImage appUserImage = appUserImageMapper.toEntity(appUserImageDTO);
        appUserImage = appUserImageRepository.save(appUserImage);
        return appUserImageMapper.toDto(appUserImage);
    }

    /**
     * Partially update a appUserImage.
     *
     * @param appUserImageDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AppUserImageDTO> partialUpdate(AppUserImageDTO appUserImageDTO) {
        LOG.debug("Request to partially update AppUserImage : {}", appUserImageDTO);

        return appUserImageRepository
            .findById(appUserImageDTO.getId())
            .map(existingAppUserImage -> {
                appUserImageMapper.partialUpdate(existingAppUserImage, appUserImageDTO);

                return existingAppUserImage;
            })
            .map(appUserImageRepository::save)
            .map(appUserImageMapper::toDto);
    }

    /**
     * Get all the appUserImages.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AppUserImageDTO> findAll() {
        LOG.debug("Request to get all AppUserImages");
        return appUserImageRepository.findAll().stream().map(appUserImageMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the appUserImages with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<AppUserImageDTO> findAllWithEagerRelationships(Pageable pageable) {
        return appUserImageRepository.findAllWithEagerRelationships(pageable).map(appUserImageMapper::toDto);
    }

    /**
     * Get one appUserImage by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AppUserImageDTO> findOne(Long id) {
        LOG.debug("Request to get AppUserImage : {}", id);
        return appUserImageRepository.findOneWithEagerRelationships(id).map(appUserImageMapper::toDto);
    }

    /**
     * Delete the appUserImage by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete AppUserImage : {}", id);
        appUserImageRepository.deleteById(id);
    }
}
