package com.partywave.backend.web.rest;

import com.partywave.backend.repository.AppUserImageRepository;
import com.partywave.backend.service.AppUserImageService;
import com.partywave.backend.service.dto.AppUserImageDTO;
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
 * REST controller for managing {@link com.partywave.backend.domain.AppUserImage}.
 */
@RestController
@RequestMapping("/api/app-user-images")
public class AppUserImageResource {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserImageResource.class);

    private static final String ENTITY_NAME = "appUserImage";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AppUserImageService appUserImageService;

    private final AppUserImageRepository appUserImageRepository;

    public AppUserImageResource(AppUserImageService appUserImageService, AppUserImageRepository appUserImageRepository) {
        this.appUserImageService = appUserImageService;
        this.appUserImageRepository = appUserImageRepository;
    }

    /**
     * {@code POST  /app-user-images} : Create a new appUserImage.
     *
     * @param appUserImageDTO the appUserImageDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new appUserImageDTO, or with status {@code 400 (Bad Request)} if the appUserImage has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AppUserImageDTO> createAppUserImage(@Valid @RequestBody AppUserImageDTO appUserImageDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save AppUserImage : {}", appUserImageDTO);
        if (appUserImageDTO.getId() != null) {
            throw new BadRequestAlertException("A new appUserImage cannot already have an ID", ENTITY_NAME, "idexists");
        }
        appUserImageDTO = appUserImageService.save(appUserImageDTO);
        return ResponseEntity.created(new URI("/api/app-user-images/" + appUserImageDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, appUserImageDTO.getId().toString()))
            .body(appUserImageDTO);
    }

    /**
     * {@code PUT  /app-user-images/:id} : Updates an existing appUserImage.
     *
     * @param id the id of the appUserImageDTO to save.
     * @param appUserImageDTO the appUserImageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated appUserImageDTO,
     * or with status {@code 400 (Bad Request)} if the appUserImageDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the appUserImageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppUserImageDTO> updateAppUserImage(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AppUserImageDTO appUserImageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AppUserImage : {}, {}", id, appUserImageDTO);
        if (appUserImageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, appUserImageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!appUserImageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        appUserImageDTO = appUserImageService.update(appUserImageDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, appUserImageDTO.getId().toString()))
            .body(appUserImageDTO);
    }

    /**
     * {@code PATCH  /app-user-images/:id} : Partial updates given fields of an existing appUserImage, field will ignore if it is null
     *
     * @param id the id of the appUserImageDTO to save.
     * @param appUserImageDTO the appUserImageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated appUserImageDTO,
     * or with status {@code 400 (Bad Request)} if the appUserImageDTO is not valid,
     * or with status {@code 404 (Not Found)} if the appUserImageDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the appUserImageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AppUserImageDTO> partialUpdateAppUserImage(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AppUserImageDTO appUserImageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AppUserImage partially : {}, {}", id, appUserImageDTO);
        if (appUserImageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, appUserImageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!appUserImageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AppUserImageDTO> result = appUserImageService.partialUpdate(appUserImageDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, appUserImageDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /app-user-images} : get all the appUserImages.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of appUserImages in body.
     */
    @GetMapping("")
    public List<AppUserImageDTO> getAllAppUserImages(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all AppUserImages");
        return appUserImageService.findAll();
    }

    /**
     * {@code GET  /app-user-images/:id} : get the "id" appUserImage.
     *
     * @param id the id of the appUserImageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the appUserImageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppUserImageDTO> getAppUserImage(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AppUserImage : {}", id);
        Optional<AppUserImageDTO> appUserImageDTO = appUserImageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(appUserImageDTO);
    }

    /**
     * {@code DELETE  /app-user-images/:id} : delete the "id" appUserImage.
     *
     * @param id the id of the appUserImageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppUserImage(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AppUserImage : {}", id);
        appUserImageService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
