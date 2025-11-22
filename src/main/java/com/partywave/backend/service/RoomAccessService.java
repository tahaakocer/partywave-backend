package com.partywave.backend.service;

import com.partywave.backend.domain.RoomAccess;
import com.partywave.backend.repository.RoomAccessRepository;
import com.partywave.backend.service.dto.RoomAccessDTO;
import com.partywave.backend.service.mapper.RoomAccessMapper;
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
 * Service Implementation for managing {@link com.partywave.backend.domain.RoomAccess}.
 */
@Service
@Transactional
public class RoomAccessService {

    private static final Logger LOG = LoggerFactory.getLogger(RoomAccessService.class);

    private final RoomAccessRepository roomAccessRepository;

    private final RoomAccessMapper roomAccessMapper;

    public RoomAccessService(RoomAccessRepository roomAccessRepository, RoomAccessMapper roomAccessMapper) {
        this.roomAccessRepository = roomAccessRepository;
        this.roomAccessMapper = roomAccessMapper;
    }

    /**
     * Save a roomAccess.
     *
     * @param roomAccessDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomAccessDTO save(RoomAccessDTO roomAccessDTO) {
        LOG.debug("Request to save RoomAccess : {}", roomAccessDTO);
        RoomAccess roomAccess = roomAccessMapper.toEntity(roomAccessDTO);
        roomAccess = roomAccessRepository.save(roomAccess);
        return roomAccessMapper.toDto(roomAccess);
    }

    /**
     * Update a roomAccess.
     *
     * @param roomAccessDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomAccessDTO update(RoomAccessDTO roomAccessDTO) {
        LOG.debug("Request to update RoomAccess : {}", roomAccessDTO);
        RoomAccess roomAccess = roomAccessMapper.toEntity(roomAccessDTO);
        roomAccess = roomAccessRepository.save(roomAccess);
        return roomAccessMapper.toDto(roomAccess);
    }

    /**
     * Partially update a roomAccess.
     *
     * @param roomAccessDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RoomAccessDTO> partialUpdate(RoomAccessDTO roomAccessDTO) {
        LOG.debug("Request to partially update RoomAccess : {}", roomAccessDTO);

        return roomAccessRepository
            .findById(roomAccessDTO.getId())
            .map(existingRoomAccess -> {
                roomAccessMapper.partialUpdate(existingRoomAccess, roomAccessDTO);

                return existingRoomAccess;
            })
            .map(roomAccessRepository::save)
            .map(roomAccessMapper::toDto);
    }

    /**
     * Get all the roomAccesses.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RoomAccessDTO> findAll() {
        LOG.debug("Request to get all RoomAccesses");
        return roomAccessRepository.findAll().stream().map(roomAccessMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the roomAccesses with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RoomAccessDTO> findAllWithEagerRelationships(Pageable pageable) {
        return roomAccessRepository.findAllWithEagerRelationships(pageable).map(roomAccessMapper::toDto);
    }

    /**
     * Get one roomAccess by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RoomAccessDTO> findOne(Long id) {
        LOG.debug("Request to get RoomAccess : {}", id);
        return roomAccessRepository.findOneWithEagerRelationships(id).map(roomAccessMapper::toDto);
    }

    /**
     * Delete the roomAccess by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete RoomAccess : {}", id);
        roomAccessRepository.deleteById(id);
    }
}
