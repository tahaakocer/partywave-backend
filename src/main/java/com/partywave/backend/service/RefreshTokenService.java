package com.partywave.backend.service;

import com.partywave.backend.domain.RefreshToken;
import com.partywave.backend.repository.RefreshTokenRepository;
import com.partywave.backend.service.dto.RefreshTokenDTO;
import com.partywave.backend.service.mapper.RefreshTokenMapper;
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
 * Service Implementation for managing {@link com.partywave.backend.domain.RefreshToken}.
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, RefreshTokenMapper refreshTokenMapper) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    /**
     * Save a refreshToken.
     *
     * @param refreshTokenDTO the entity to save.
     * @return the persisted entity.
     */
    public RefreshTokenDTO save(RefreshTokenDTO refreshTokenDTO) {
        LOG.debug("Request to save RefreshToken : {}", refreshTokenDTO);
        RefreshToken refreshToken = refreshTokenMapper.toEntity(refreshTokenDTO);
        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshTokenMapper.toDto(refreshToken);
    }

    /**
     * Update a refreshToken.
     *
     * @param refreshTokenDTO the entity to save.
     * @return the persisted entity.
     */
    public RefreshTokenDTO update(RefreshTokenDTO refreshTokenDTO) {
        LOG.debug("Request to update RefreshToken : {}", refreshTokenDTO);
        RefreshToken refreshToken = refreshTokenMapper.toEntity(refreshTokenDTO);
        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshTokenMapper.toDto(refreshToken);
    }

    /**
     * Partially update a refreshToken.
     *
     * @param refreshTokenDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RefreshTokenDTO> partialUpdate(RefreshTokenDTO refreshTokenDTO) {
        LOG.debug("Request to partially update RefreshToken : {}", refreshTokenDTO);

        return refreshTokenRepository
            .findById(refreshTokenDTO.getId())
            .map(existingRefreshToken -> {
                refreshTokenMapper.partialUpdate(existingRefreshToken, refreshTokenDTO);

                return existingRefreshToken;
            })
            .map(refreshTokenRepository::save)
            .map(refreshTokenMapper::toDto);
    }

    /**
     * Get all the refreshTokens.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RefreshTokenDTO> findAll() {
        LOG.debug("Request to get all RefreshTokens");
        return refreshTokenRepository.findAll().stream().map(refreshTokenMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the refreshTokens with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RefreshTokenDTO> findAllWithEagerRelationships(Pageable pageable) {
        return refreshTokenRepository.findAllWithEagerRelationships(pageable).map(refreshTokenMapper::toDto);
    }

    /**
     * Get one refreshToken by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RefreshTokenDTO> findOne(Long id) {
        LOG.debug("Request to get RefreshToken : {}", id);
        return refreshTokenRepository.findOneWithEagerRelationships(id).map(refreshTokenMapper::toDto);
    }

    /**
     * Delete the refreshToken by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete RefreshToken : {}", id);
        refreshTokenRepository.deleteById(id);
    }
}
