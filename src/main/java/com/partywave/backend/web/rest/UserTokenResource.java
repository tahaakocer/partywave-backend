package com.partywave.backend.web.rest;

import com.partywave.backend.repository.UserTokenRepository;
import com.partywave.backend.service.UserTokenService;
import com.partywave.backend.service.dto.UserTokenDTO;
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
 * REST controller for managing {@link com.partywave.backend.domain.UserToken}.
 */
@RestController
@RequestMapping("/api/user-tokens")
public class UserTokenResource {

    private static final Logger LOG = LoggerFactory.getLogger(UserTokenResource.class);

    private static final String ENTITY_NAME = "userToken";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserTokenService userTokenService;

    private final UserTokenRepository userTokenRepository;

    public UserTokenResource(UserTokenService userTokenService, UserTokenRepository userTokenRepository) {
        this.userTokenService = userTokenService;
        this.userTokenRepository = userTokenRepository;
    }

    /**
     * {@code POST  /user-tokens} : Create a new userToken.
     *
     * @param userTokenDTO the userTokenDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userTokenDTO, or with status {@code 400 (Bad Request)} if the userToken has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<UserTokenDTO> createUserToken(@Valid @RequestBody UserTokenDTO userTokenDTO) throws URISyntaxException {
        LOG.debug("REST request to save UserToken : {}", userTokenDTO);
        if (userTokenDTO.getId() != null) {
            throw new BadRequestAlertException("A new userToken cannot already have an ID", ENTITY_NAME, "idexists");
        }
        userTokenDTO = userTokenService.save(userTokenDTO);
        return ResponseEntity.created(new URI("/api/user-tokens/" + userTokenDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, userTokenDTO.getId().toString()))
            .body(userTokenDTO);
    }

    /**
     * {@code PUT  /user-tokens/:id} : Updates an existing userToken.
     *
     * @param id the id of the userTokenDTO to save.
     * @param userTokenDTO the userTokenDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userTokenDTO,
     * or with status {@code 400 (Bad Request)} if the userTokenDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the userTokenDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserTokenDTO> updateUserToken(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody UserTokenDTO userTokenDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update UserToken : {}, {}", id, userTokenDTO);
        if (userTokenDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userTokenDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userTokenRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        userTokenDTO = userTokenService.update(userTokenDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userTokenDTO.getId().toString()))
            .body(userTokenDTO);
    }

    /**
     * {@code PATCH  /user-tokens/:id} : Partial updates given fields of an existing userToken, field will ignore if it is null
     *
     * @param id the id of the userTokenDTO to save.
     * @param userTokenDTO the userTokenDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userTokenDTO,
     * or with status {@code 400 (Bad Request)} if the userTokenDTO is not valid,
     * or with status {@code 404 (Not Found)} if the userTokenDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the userTokenDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<UserTokenDTO> partialUpdateUserToken(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody UserTokenDTO userTokenDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update UserToken partially : {}, {}", id, userTokenDTO);
        if (userTokenDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userTokenDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userTokenRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UserTokenDTO> result = userTokenService.partialUpdate(userTokenDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userTokenDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /user-tokens} : get all the userTokens.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of userTokens in body.
     */
    @GetMapping("")
    public List<UserTokenDTO> getAllUserTokens(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all UserTokens");
        return userTokenService.findAll();
    }

    /**
     * {@code GET  /user-tokens/:id} : get the "id" userToken.
     *
     * @param id the id of the userTokenDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userTokenDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserTokenDTO> getUserToken(@PathVariable("id") Long id) {
        LOG.debug("REST request to get UserToken : {}", id);
        Optional<UserTokenDTO> userTokenDTO = userTokenService.findOne(id);
        return ResponseUtil.wrapOrNotFound(userTokenDTO);
    }

    /**
     * {@code DELETE  /user-tokens/:id} : delete the "id" userToken.
     *
     * @param id the id of the userTokenDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserToken(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete UserToken : {}", id);
        userTokenService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
