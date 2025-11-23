package com.partywave.backend.service;

import com.partywave.backend.domain.Vote;
import com.partywave.backend.repository.VoteRepository;
import com.partywave.backend.service.dto.VoteDTO;
import com.partywave.backend.service.mapper.VoteMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.partywave.backend.domain.Vote}.
 */
@Service
@Transactional
public class VoteService {

    private static final Logger LOG = LoggerFactory.getLogger(VoteService.class);

    private final VoteRepository voteRepository;

    private final VoteMapper voteMapper;

    public VoteService(VoteRepository voteRepository, VoteMapper voteMapper) {
        this.voteRepository = voteRepository;
        this.voteMapper = voteMapper;
    }

    /**
     * Save a vote.
     *
     * @param voteDTO the entity to save.
     * @return the persisted entity.
     */
    public VoteDTO save(VoteDTO voteDTO) {
        LOG.debug("Request to save Vote : {}", voteDTO);
        Vote vote = voteMapper.toEntity(voteDTO);
        vote = voteRepository.save(vote);
        return voteMapper.toDto(vote);
    }

    /**
     * Update a vote.
     *
     * @param voteDTO the entity to save.
     * @return the persisted entity.
     */
    public VoteDTO update(VoteDTO voteDTO) {
        LOG.debug("Request to update Vote : {}", voteDTO);
        Vote vote = voteMapper.toEntity(voteDTO);
        vote = voteRepository.save(vote);
        return voteMapper.toDto(vote);
    }

    /**
     * Partially update a vote.
     *
     * @param voteDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<VoteDTO> partialUpdate(VoteDTO voteDTO) {
        LOG.debug("Request to partially update Vote : {}", voteDTO);

        return voteRepository
            .findById(voteDTO.getId())
            .map(existingVote -> {
                voteMapper.partialUpdate(existingVote, voteDTO);

                return existingVote;
            })
            .map(voteRepository::save)
            .map(voteMapper::toDto);
    }

    /**
     * Get all the votes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<VoteDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Votes");
        return voteRepository.findAll(pageable).map(voteMapper::toDto);
    }

    /**
     * Get all the votes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<VoteDTO> findAllWithEagerRelationships(Pageable pageable) {
        return voteRepository.findAllWithEagerRelationships(pageable).map(voteMapper::toDto);
    }

    /**
     * Get one vote by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<VoteDTO> findOne(UUID id) {
        LOG.debug("Request to get Vote : {}", id);
        return voteRepository.findOneWithEagerRelationships(id).map(voteMapper::toDto);
    }

    /**
     * Delete the vote by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Vote : {}", id);
        voteRepository.deleteById(id);
    }
}
