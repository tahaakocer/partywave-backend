package com.partywave.backend.service;

import com.partywave.backend.domain.RoomInvitation;
import com.partywave.backend.repository.RoomInvitationRepository;
import com.partywave.backend.service.dto.RoomInvitationDTO;
import com.partywave.backend.service.mapper.RoomInvitationMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.partywave.backend.domain.RoomInvitation}.
 */
@Service
@Transactional
public class RoomInvitationService {

    private static final Logger LOG = LoggerFactory.getLogger(RoomInvitationService.class);

    private final RoomInvitationRepository roomInvitationRepository;

    private final RoomInvitationMapper roomInvitationMapper;

    public RoomInvitationService(RoomInvitationRepository roomInvitationRepository, RoomInvitationMapper roomInvitationMapper) {
        this.roomInvitationRepository = roomInvitationRepository;
        this.roomInvitationMapper = roomInvitationMapper;
    }

    /**
     * Save a roomInvitation.
     *
     * @param roomInvitationDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomInvitationDTO save(RoomInvitationDTO roomInvitationDTO) {
        LOG.debug("Request to save RoomInvitation : {}", roomInvitationDTO);
        RoomInvitation roomInvitation = roomInvitationMapper.toEntity(roomInvitationDTO);
        roomInvitation = roomInvitationRepository.save(roomInvitation);
        return roomInvitationMapper.toDto(roomInvitation);
    }

    /**
     * Update a roomInvitation.
     *
     * @param roomInvitationDTO the entity to save.
     * @return the persisted entity.
     */
    public RoomInvitationDTO update(RoomInvitationDTO roomInvitationDTO) {
        LOG.debug("Request to update RoomInvitation : {}", roomInvitationDTO);
        RoomInvitation roomInvitation = roomInvitationMapper.toEntity(roomInvitationDTO);
        roomInvitation = roomInvitationRepository.save(roomInvitation);
        return roomInvitationMapper.toDto(roomInvitation);
    }

    /**
     * Partially update a roomInvitation.
     *
     * @param roomInvitationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RoomInvitationDTO> partialUpdate(RoomInvitationDTO roomInvitationDTO) {
        LOG.debug("Request to partially update RoomInvitation : {}", roomInvitationDTO);

        return roomInvitationRepository
            .findById(roomInvitationDTO.getId())
            .map(existingRoomInvitation -> {
                roomInvitationMapper.partialUpdate(existingRoomInvitation, roomInvitationDTO);

                return existingRoomInvitation;
            })
            .map(roomInvitationRepository::save)
            .map(roomInvitationMapper::toDto);
    }

    /**
     * Get all the roomInvitations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RoomInvitationDTO> findAll() {
        LOG.debug("Request to get all RoomInvitations");
        return roomInvitationRepository
            .findAll()
            .stream()
            .map(roomInvitationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the roomInvitations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RoomInvitationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return roomInvitationRepository.findAllWithEagerRelationships(pageable).map(roomInvitationMapper::toDto);
    }

    /**
     * Get one roomInvitation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RoomInvitationDTO> findOne(UUID id) {
        LOG.debug("Request to get RoomInvitation : {}", id);
        return roomInvitationRepository.findOneWithEagerRelationships(id).map(roomInvitationMapper::toDto);
    }

    /**
     * Delete the roomInvitation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RoomInvitation : {}", id);
        roomInvitationRepository.deleteById(id);
    }
}
