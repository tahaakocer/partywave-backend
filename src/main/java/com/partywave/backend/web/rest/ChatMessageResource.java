package com.partywave.backend.web.rest;

import com.partywave.backend.repository.ChatMessageRepository;
import com.partywave.backend.service.ChatMessageService;
import com.partywave.backend.service.dto.ChatMessageDTO;
import com.partywave.backend.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.partywave.backend.domain.ChatMessage}.
 */
@RestController
@RequestMapping("/api/chat-messages")
public class ChatMessageResource {

    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageResource.class);

    private static final String ENTITY_NAME = "chatMessage";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ChatMessageService chatMessageService;

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageResource(ChatMessageService chatMessageService, ChatMessageRepository chatMessageRepository) {
        this.chatMessageService = chatMessageService;
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * {@code POST  /chat-messages} : Create a new chatMessage.
     *
     * @param chatMessageDTO the chatMessageDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new chatMessageDTO, or with status {@code 400 (Bad Request)} if the chatMessage has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ChatMessageDTO> createChatMessage(@Valid @RequestBody ChatMessageDTO chatMessageDTO) throws URISyntaxException {
        LOG.debug("REST request to save ChatMessage : {}", chatMessageDTO);
        if (chatMessageDTO.getId() != null) {
            throw new BadRequestAlertException("A new chatMessage cannot already have an ID", ENTITY_NAME, "idexists");
        }
        chatMessageDTO = chatMessageService.save(chatMessageDTO);
        return ResponseEntity.created(new URI("/api/chat-messages/" + chatMessageDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, chatMessageDTO.getId().toString()))
            .body(chatMessageDTO);
    }

    /**
     * {@code PUT  /chat-messages/:id} : Updates an existing chatMessage.
     *
     * @param id the id of the chatMessageDTO to save.
     * @param chatMessageDTO the chatMessageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated chatMessageDTO,
     * or with status {@code 400 (Bad Request)} if the chatMessageDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the chatMessageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChatMessageDTO> updateChatMessage(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ChatMessageDTO chatMessageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ChatMessage : {}, {}", id, chatMessageDTO);
        if (chatMessageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, chatMessageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!chatMessageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        chatMessageDTO = chatMessageService.update(chatMessageDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, chatMessageDTO.getId().toString()))
            .body(chatMessageDTO);
    }

    /**
     * {@code PATCH  /chat-messages/:id} : Partial updates given fields of an existing chatMessage, field will ignore if it is null
     *
     * @param id the id of the chatMessageDTO to save.
     * @param chatMessageDTO the chatMessageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated chatMessageDTO,
     * or with status {@code 400 (Bad Request)} if the chatMessageDTO is not valid,
     * or with status {@code 404 (Not Found)} if the chatMessageDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the chatMessageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ChatMessageDTO> partialUpdateChatMessage(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ChatMessageDTO chatMessageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ChatMessage partially : {}, {}", id, chatMessageDTO);
        if (chatMessageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, chatMessageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!chatMessageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ChatMessageDTO> result = chatMessageService.partialUpdate(chatMessageDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, chatMessageDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /chat-messages} : get all the chatMessages.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of chatMessages in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ChatMessageDTO>> getAllChatMessages(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of ChatMessages");
        Page<ChatMessageDTO> page;
        if (eagerload) {
            page = chatMessageService.findAllWithEagerRelationships(pageable);
        } else {
            page = chatMessageService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /chat-messages/:id} : get the "id" chatMessage.
     *
     * @param id the id of the chatMessageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the chatMessageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChatMessageDTO> getChatMessage(@PathVariable("id") Long id) {
        LOG.debug("REST request to get ChatMessage : {}", id);
        Optional<ChatMessageDTO> chatMessageDTO = chatMessageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(chatMessageDTO);
    }

    /**
     * {@code DELETE  /chat-messages/:id} : delete the "id" chatMessage.
     *
     * @param id the id of the chatMessageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatMessage(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete ChatMessage : {}", id);
        chatMessageService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
