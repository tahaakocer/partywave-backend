package com.partywave.backend.web.rest;

import com.partywave.backend.repository.AppUserStatsRepository;
import com.partywave.backend.service.AppUserStatsService;
import com.partywave.backend.service.dto.AppUserStatsDTO;
import com.partywave.backend.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link com.partywave.backend.domain.AppUserStats}.
 */
@RestController
@RequestMapping("/api/app-user-stats")
public class AppUserStatsResource {

    private static final Logger LOG = LoggerFactory.getLogger(AppUserStatsResource.class);

    private static final String ENTITY_NAME = "appUserStats";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AppUserStatsService appUserStatsService;

    private final AppUserStatsRepository appUserStatsRepository;

    public AppUserStatsResource(AppUserStatsService appUserStatsService, AppUserStatsRepository appUserStatsRepository) {
        this.appUserStatsService = appUserStatsService;
        this.appUserStatsRepository = appUserStatsRepository;
    }

    /**
     * {@code POST  /app-user-stats} : Create a new appUserStats.
     *
     * @param appUserStatsDTO the appUserStatsDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new appUserStatsDTO, or with status {@code 400 (Bad Request)} if the appUserStats has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AppUserStatsDTO> createAppUserStats(@RequestBody AppUserStatsDTO appUserStatsDTO) throws URISyntaxException {
        LOG.debug("REST request to save AppUserStats : {}", appUserStatsDTO);
        if (appUserStatsDTO.getId() != null) {
            throw new BadRequestAlertException("A new appUserStats cannot already have an ID", ENTITY_NAME, "idexists");
        }
        appUserStatsDTO = appUserStatsService.save(appUserStatsDTO);
        return ResponseEntity.created(new URI("/api/app-user-stats/" + appUserStatsDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, appUserStatsDTO.getId().toString()))
            .body(appUserStatsDTO);
    }

    /**
     * {@code PUT  /app-user-stats/:id} : Updates an existing appUserStats.
     *
     * @param id the id of the appUserStatsDTO to save.
     * @param appUserStatsDTO the appUserStatsDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated appUserStatsDTO,
     * or with status {@code 400 (Bad Request)} if the appUserStatsDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the appUserStatsDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppUserStatsDTO> updateAppUserStats(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody AppUserStatsDTO appUserStatsDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AppUserStats : {}, {}", id, appUserStatsDTO);
        if (appUserStatsDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, appUserStatsDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!appUserStatsRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        appUserStatsDTO = appUserStatsService.update(appUserStatsDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, appUserStatsDTO.getId().toString()))
            .body(appUserStatsDTO);
    }

    /**
     * {@code PATCH  /app-user-stats/:id} : Partial updates given fields of an existing appUserStats, field will ignore if it is null
     *
     * @param id the id of the appUserStatsDTO to save.
     * @param appUserStatsDTO the appUserStatsDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated appUserStatsDTO,
     * or with status {@code 400 (Bad Request)} if the appUserStatsDTO is not valid,
     * or with status {@code 404 (Not Found)} if the appUserStatsDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the appUserStatsDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AppUserStatsDTO> partialUpdateAppUserStats(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody AppUserStatsDTO appUserStatsDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AppUserStats partially : {}, {}", id, appUserStatsDTO);
        if (appUserStatsDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, appUserStatsDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!appUserStatsRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AppUserStatsDTO> result = appUserStatsService.partialUpdate(appUserStatsDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, appUserStatsDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /app-user-stats} : get all the appUserStats.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of appUserStats in body.
     */
    @GetMapping("")
    public List<AppUserStatsDTO> getAllAppUserStats(@RequestParam(name = "filter", required = false) String filter) {
        if ("appuser-is-null".equals(filter)) {
            LOG.debug("REST request to get all AppUserStatss where appUser is null");
            return appUserStatsService.findAllWhereAppUserIsNull();
        }
        LOG.debug("REST request to get all AppUserStats");
        return appUserStatsService.findAll();
    }

    /**
     * {@code GET  /app-user-stats/:id} : get the "id" appUserStats.
     *
     * @param id the id of the appUserStatsDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the appUserStatsDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppUserStatsDTO> getAppUserStats(@PathVariable("id") Long id) {
        LOG.debug("REST request to get AppUserStats : {}", id);
        Optional<AppUserStatsDTO> appUserStatsDTO = appUserStatsService.findOne(id);
        return ResponseUtil.wrapOrNotFound(appUserStatsDTO);
    }

    /**
     * {@code DELETE  /app-user-stats/:id} : delete the "id" appUserStats.
     *
     * @param id the id of the appUserStatsDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppUserStats(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete AppUserStats : {}", id);
        appUserStatsService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
