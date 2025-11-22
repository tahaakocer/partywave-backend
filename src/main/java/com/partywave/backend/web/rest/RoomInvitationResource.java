package com.partywave.backend.web.rest;

import com.partywave.backend.repository.RoomInvitationRepository;
import com.partywave.backend.service.RoomInvitationService;
import com.partywave.backend.service.dto.RoomInvitationDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.partywave.backend.domain.RoomInvitation}.
 */
@RestController
@RequestMapping("/api/room-invitations")
public class RoomInvitationResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomInvitationResource.class);

    private static final String ENTITY_NAME = "roomInvitation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RoomInvitationService roomInvitationService;

    private final RoomInvitationRepository roomInvitationRepository;

    public RoomInvitationResource(RoomInvitationService roomInvitationService, RoomInvitationRepository roomInvitationRepository) {
        this.roomInvitationService = roomInvitationService;
        this.roomInvitationRepository = roomInvitationRepository;
    }

    /**
     * {@code POST  /room-invitations} : Create a new roomInvitation.
     *
     * @param roomInvitationDTO the roomInvitationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new roomInvitationDTO, or with status {@code 400 (Bad Request)} if the roomInvitation has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RoomInvitationDTO> createRoomInvitation(@Valid @RequestBody RoomInvitationDTO roomInvitationDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RoomInvitation : {}", roomInvitationDTO);
        if (roomInvitationDTO.getId() != null) {
            throw new BadRequestAlertException("A new roomInvitation cannot already have an ID", ENTITY_NAME, "idexists");
        }
        roomInvitationDTO = roomInvitationService.save(roomInvitationDTO);
        return ResponseEntity.created(new URI("/api/room-invitations/" + roomInvitationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, roomInvitationDTO.getId().toString()))
            .body(roomInvitationDTO);
    }

    /**
     * {@code PUT  /room-invitations/:id} : Updates an existing roomInvitation.
     *
     * @param id the id of the roomInvitationDTO to save.
     * @param roomInvitationDTO the roomInvitationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomInvitationDTO,
     * or with status {@code 400 (Bad Request)} if the roomInvitationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the roomInvitationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoomInvitationDTO> updateRoomInvitation(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RoomInvitationDTO roomInvitationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RoomInvitation : {}, {}", id, roomInvitationDTO);
        if (roomInvitationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomInvitationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomInvitationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        roomInvitationDTO = roomInvitationService.update(roomInvitationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomInvitationDTO.getId().toString()))
            .body(roomInvitationDTO);
    }

    /**
     * {@code PATCH  /room-invitations/:id} : Partial updates given fields of an existing roomInvitation, field will ignore if it is null
     *
     * @param id the id of the roomInvitationDTO to save.
     * @param roomInvitationDTO the roomInvitationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomInvitationDTO,
     * or with status {@code 400 (Bad Request)} if the roomInvitationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the roomInvitationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the roomInvitationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RoomInvitationDTO> partialUpdateRoomInvitation(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RoomInvitationDTO roomInvitationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RoomInvitation partially : {}, {}", id, roomInvitationDTO);
        if (roomInvitationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomInvitationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomInvitationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RoomInvitationDTO> result = roomInvitationService.partialUpdate(roomInvitationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomInvitationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /room-invitations} : get all the roomInvitations.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of roomInvitations in body.
     */
    @GetMapping("")
    public List<RoomInvitationDTO> getAllRoomInvitations(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RoomInvitations");
        return roomInvitationService.findAll();
    }

    /**
     * {@code GET  /room-invitations/:id} : get the "id" roomInvitation.
     *
     * @param id the id of the roomInvitationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roomInvitationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomInvitationDTO> getRoomInvitation(@PathVariable("id") Long id) {
        LOG.debug("REST request to get RoomInvitation : {}", id);
        Optional<RoomInvitationDTO> roomInvitationDTO = roomInvitationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roomInvitationDTO);
    }

    /**
     * {@code DELETE  /room-invitations/:id} : delete the "id" roomInvitation.
     *
     * @param id the id of the roomInvitationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomInvitation(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete RoomInvitation : {}", id);
        roomInvitationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
