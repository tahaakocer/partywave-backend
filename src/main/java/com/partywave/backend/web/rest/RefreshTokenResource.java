package com.partywave.backend.web.rest;

import com.partywave.backend.repository.RefreshTokenRepository;
import com.partywave.backend.service.RefreshTokenService;
import com.partywave.backend.service.dto.RefreshTokenDTO;
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
 * REST controller for managing {@link com.partywave.backend.domain.RefreshToken}.
 */
@RestController
@RequestMapping("/api/refresh-tokens")
public class RefreshTokenResource {

    private static final Logger LOG = LoggerFactory.getLogger(RefreshTokenResource.class);

    private static final String ENTITY_NAME = "refreshToken";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RefreshTokenService refreshTokenService;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenResource(RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * {@code POST  /refresh-tokens} : Create a new refreshToken.
     *
     * @param refreshTokenDTO the refreshTokenDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new refreshTokenDTO, or with status {@code 400 (Bad Request)} if the refreshToken has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RefreshTokenDTO> createRefreshToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RefreshToken : {}", refreshTokenDTO);
        if (refreshTokenDTO.getId() != null) {
            throw new BadRequestAlertException("A new refreshToken cannot already have an ID", ENTITY_NAME, "idexists");
        }
        refreshTokenDTO = refreshTokenService.save(refreshTokenDTO);
        return ResponseEntity.created(new URI("/api/refresh-tokens/" + refreshTokenDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, refreshTokenDTO.getId().toString()))
            .body(refreshTokenDTO);
    }

    /**
     * {@code PUT  /refresh-tokens/:id} : Updates an existing refreshToken.
     *
     * @param id the id of the refreshTokenDTO to save.
     * @param refreshTokenDTO the refreshTokenDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated refreshTokenDTO,
     * or with status {@code 400 (Bad Request)} if the refreshTokenDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the refreshTokenDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RefreshTokenDTO> updateRefreshToken(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RefreshTokenDTO refreshTokenDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RefreshToken : {}, {}", id, refreshTokenDTO);
        if (refreshTokenDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, refreshTokenDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!refreshTokenRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        refreshTokenDTO = refreshTokenService.update(refreshTokenDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, refreshTokenDTO.getId().toString()))
            .body(refreshTokenDTO);
    }

    /**
     * {@code PATCH  /refresh-tokens/:id} : Partial updates given fields of an existing refreshToken, field will ignore if it is null
     *
     * @param id the id of the refreshTokenDTO to save.
     * @param refreshTokenDTO the refreshTokenDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated refreshTokenDTO,
     * or with status {@code 400 (Bad Request)} if the refreshTokenDTO is not valid,
     * or with status {@code 404 (Not Found)} if the refreshTokenDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the refreshTokenDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RefreshTokenDTO> partialUpdateRefreshToken(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RefreshTokenDTO refreshTokenDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RefreshToken partially : {}, {}", id, refreshTokenDTO);
        if (refreshTokenDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, refreshTokenDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!refreshTokenRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RefreshTokenDTO> result = refreshTokenService.partialUpdate(refreshTokenDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, refreshTokenDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /refresh-tokens} : get all the refreshTokens.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of refreshTokens in body.
     */
    @GetMapping("")
    public List<RefreshTokenDTO> getAllRefreshTokens(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RefreshTokens");
        return refreshTokenService.findAll();
    }

    /**
     * {@code GET  /refresh-tokens/:id} : get the "id" refreshToken.
     *
     * @param id the id of the refreshTokenDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the refreshTokenDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RefreshTokenDTO> getRefreshToken(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RefreshToken : {}", id);
        Optional<RefreshTokenDTO> refreshTokenDTO = refreshTokenService.findOne(id);
        return ResponseUtil.wrapOrNotFound(refreshTokenDTO);
    }

    /**
     * {@code DELETE  /refresh-tokens/:id} : delete the "id" refreshToken.
     *
     * @param id the id of the refreshTokenDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRefreshToken(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete RefreshToken : {}", id);
        refreshTokenService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
