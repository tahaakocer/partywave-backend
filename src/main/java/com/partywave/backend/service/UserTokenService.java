package com.partywave.backend.service;

import com.partywave.backend.domain.UserToken;
import com.partywave.backend.repository.UserTokenRepository;
import com.partywave.backend.service.dto.UserTokenDTO;
import com.partywave.backend.service.mapper.UserTokenMapper;
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
 * Service Implementation for managing {@link com.partywave.backend.domain.UserToken}.
 */
@Service
@Transactional
public class UserTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(UserTokenService.class);

    private final UserTokenRepository userTokenRepository;

    private final UserTokenMapper userTokenMapper;

    public UserTokenService(UserTokenRepository userTokenRepository, UserTokenMapper userTokenMapper) {
        this.userTokenRepository = userTokenRepository;
        this.userTokenMapper = userTokenMapper;
    }

    /**
     * Save a userToken.
     *
     * @param userTokenDTO the entity to save.
     * @return the persisted entity.
     */
    public UserTokenDTO save(UserTokenDTO userTokenDTO) {
        LOG.debug("Request to save UserToken : {}", userTokenDTO);
        UserToken userToken = userTokenMapper.toEntity(userTokenDTO);
        userToken = userTokenRepository.save(userToken);
        return userTokenMapper.toDto(userToken);
    }

    /**
     * Update a userToken.
     *
     * @param userTokenDTO the entity to save.
     * @return the persisted entity.
     */
    public UserTokenDTO update(UserTokenDTO userTokenDTO) {
        LOG.debug("Request to update UserToken : {}", userTokenDTO);
        UserToken userToken = userTokenMapper.toEntity(userTokenDTO);
        userToken = userTokenRepository.save(userToken);
        return userTokenMapper.toDto(userToken);
    }

    /**
     * Partially update a userToken.
     *
     * @param userTokenDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<UserTokenDTO> partialUpdate(UserTokenDTO userTokenDTO) {
        LOG.debug("Request to partially update UserToken : {}", userTokenDTO);

        return userTokenRepository
            .findById(userTokenDTO.getId())
            .map(existingUserToken -> {
                userTokenMapper.partialUpdate(existingUserToken, userTokenDTO);

                return existingUserToken;
            })
            .map(userTokenRepository::save)
            .map(userTokenMapper::toDto);
    }

    /**
     * Get all the userTokens.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<UserTokenDTO> findAll() {
        LOG.debug("Request to get all UserTokens");
        return userTokenRepository.findAll().stream().map(userTokenMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the userTokens with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<UserTokenDTO> findAllWithEagerRelationships(Pageable pageable) {
        return userTokenRepository.findAllWithEagerRelationships(pageable).map(userTokenMapper::toDto);
    }

    /**
     * Get one userToken by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<UserTokenDTO> findOne(Long id) {
        LOG.debug("Request to get UserToken : {}", id);
        return userTokenRepository.findOneWithEagerRelationships(id).map(userTokenMapper::toDto);
    }

    /**
     * Delete the userToken by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete UserToken : {}", id);
        userTokenRepository.deleteById(id);
    }
}
