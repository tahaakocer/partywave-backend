package com.partywave.backend.service;

import com.partywave.backend.domain.ChatMessage;
import com.partywave.backend.repository.ChatMessageRepository;
import com.partywave.backend.service.dto.ChatMessageDTO;
import com.partywave.backend.service.mapper.ChatMessageMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.partywave.backend.domain.ChatMessage}.
 */
@Service
@Transactional
public class ChatMessageService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageRepository chatMessageRepository;

    private final ChatMessageMapper chatMessageMapper;

    public ChatMessageService(ChatMessageRepository chatMessageRepository, ChatMessageMapper chatMessageMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageMapper = chatMessageMapper;
    }

    /**
     * Save a chatMessage.
     *
     * @param chatMessageDTO the entity to save.
     * @return the persisted entity.
     */
    public ChatMessageDTO save(ChatMessageDTO chatMessageDTO) {
        LOG.debug("Request to save ChatMessage : {}", chatMessageDTO);
        ChatMessage chatMessage = chatMessageMapper.toEntity(chatMessageDTO);
        chatMessage = chatMessageRepository.save(chatMessage);
        return chatMessageMapper.toDto(chatMessage);
    }

    /**
     * Update a chatMessage.
     *
     * @param chatMessageDTO the entity to save.
     * @return the persisted entity.
     */
    public ChatMessageDTO update(ChatMessageDTO chatMessageDTO) {
        LOG.debug("Request to update ChatMessage : {}", chatMessageDTO);
        ChatMessage chatMessage = chatMessageMapper.toEntity(chatMessageDTO);
        chatMessage = chatMessageRepository.save(chatMessage);
        return chatMessageMapper.toDto(chatMessage);
    }

    /**
     * Partially update a chatMessage.
     *
     * @param chatMessageDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ChatMessageDTO> partialUpdate(ChatMessageDTO chatMessageDTO) {
        LOG.debug("Request to partially update ChatMessage : {}", chatMessageDTO);

        return chatMessageRepository
            .findById(chatMessageDTO.getId())
            .map(existingChatMessage -> {
                chatMessageMapper.partialUpdate(existingChatMessage, chatMessageDTO);

                return existingChatMessage;
            })
            .map(chatMessageRepository::save)
            .map(chatMessageMapper::toDto);
    }

    /**
     * Get all the chatMessages.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all ChatMessages");
        return chatMessageRepository.findAll(pageable).map(chatMessageMapper::toDto);
    }

    /**
     * Get all the chatMessages with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ChatMessageDTO> findAllWithEagerRelationships(Pageable pageable) {
        return chatMessageRepository.findAllWithEagerRelationships(pageable).map(chatMessageMapper::toDto);
    }

    /**
     * Get one chatMessage by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ChatMessageDTO> findOne(Long id) {
        LOG.debug("Request to get ChatMessage : {}", id);
        return chatMessageRepository.findOneWithEagerRelationships(id).map(chatMessageMapper::toDto);
    }

    /**
     * Delete the chatMessage by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete ChatMessage : {}", id);
        chatMessageRepository.deleteById(id);
    }
}
