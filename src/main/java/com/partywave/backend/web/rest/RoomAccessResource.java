package com.partywave.backend.web.rest;

import com.partywave.backend.repository.RoomAccessRepository;
import com.partywave.backend.service.RoomAccessService;
import com.partywave.backend.service.dto.RoomAccessDTO;
import com.partywave.backend.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.partywave.backend.domain.RoomAccess}.
 */
@RestController
@RequestMapping("/api/room-accesses")
public class RoomAccessResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomAccessResource.class);

    private static final String ENTITY_NAME = "roomAccess";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RoomAccessService roomAccessService;

    private final RoomAccessRepository roomAccessRepository;

    public RoomAccessResource(RoomAccessService roomAccessService, RoomAccessRepository roomAccessRepository) {
        this.roomAccessService = roomAccessService;
        this.roomAccessRepository = roomAccessRepository;
    }

    /**
     * {@code POST  /room-accesses} : Create a new roomAccess.
     *
     * @param roomAccessDTO the roomAccessDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new roomAccessDTO, or with status {@code 400 (Bad Request)} if the roomAccess has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RoomAccessDTO> createRoomAccess(@Valid @RequestBody RoomAccessDTO roomAccessDTO) throws URISyntaxException {
        LOG.debug("REST request to save RoomAccess : {}", roomAccessDTO);
        if (roomAccessDTO.getId() != null) {
            throw new BadRequestAlertException("A new roomAccess cannot already have an ID", ENTITY_NAME, "idexists");
        }
        roomAccessDTO = roomAccessService.save(roomAccessDTO);
        return ResponseEntity.created(new URI("/api/room-accesses/" + roomAccessDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, roomAccessDTO.getId().toString()))
            .body(roomAccessDTO);
    }

    /**
     * {@code PUT  /room-accesses/:id} : Updates an existing roomAccess.
     *
     * @param id the id of the roomAccessDTO to save.
     * @param roomAccessDTO the roomAccessDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomAccessDTO,
     * or with status {@code 400 (Bad Request)} if the roomAccessDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the roomAccessDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoomAccessDTO> updateRoomAccess(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RoomAccessDTO roomAccessDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RoomAccess : {}, {}", id, roomAccessDTO);
        if (roomAccessDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomAccessDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomAccessRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        roomAccessDTO = roomAccessService.update(roomAccessDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomAccessDTO.getId().toString()))
            .body(roomAccessDTO);
    }

    /**
     * {@code PATCH  /room-accesses/:id} : Partial updates given fields of an existing roomAccess, field will ignore if it is null
     *
     * @param id the id of the roomAccessDTO to save.
     * @param roomAccessDTO the roomAccessDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomAccessDTO,
     * or with status {@code 400 (Bad Request)} if the roomAccessDTO is not valid,
     * or with status {@code 404 (Not Found)} if the roomAccessDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the roomAccessDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RoomAccessDTO> partialUpdateRoomAccess(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RoomAccessDTO roomAccessDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RoomAccess partially : {}, {}", id, roomAccessDTO);
        if (roomAccessDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomAccessDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomAccessRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RoomAccessDTO> result = roomAccessService.partialUpdate(roomAccessDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomAccessDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /room-accesses} : get all the roomAccesses.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of roomAccesses in body.
     */
    @GetMapping("")
    public List<RoomAccessDTO> getAllRoomAccesses(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RoomAccesses");
        return roomAccessService.findAll();
    }

    /**
     * {@code GET  /room-accesses/:id} : get the "id" roomAccess.
     *
     * @param id the id of the roomAccessDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roomAccessDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomAccessDTO> getRoomAccess(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RoomAccess : {}", id);
        Optional<RoomAccessDTO> roomAccessDTO = roomAccessService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roomAccessDTO);
    }

    /**
     * {@code DELETE  /room-accesses/:id} : delete the "id" roomAccess.
     *
     * @param id the id of the roomAccessDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomAccess(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete RoomAccess : {}", id);
        roomAccessService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
