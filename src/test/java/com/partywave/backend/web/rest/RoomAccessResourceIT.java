package com.partywave.backend.web.rest;

import static com.partywave.backend.domain.RoomAccessAsserts.*;
import static com.partywave.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.IntegrationTest;
import com.partywave.backend.domain.RoomAccess;
import com.partywave.backend.repository.RoomAccessRepository;
import com.partywave.backend.service.RoomAccessService;
import com.partywave.backend.service.dto.RoomAccessDTO;
import com.partywave.backend.service.mapper.RoomAccessMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link RoomAccessResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RoomAccessResourceIT {

    private static final Instant DEFAULT_GRANTED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_GRANTED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/room-accesses";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoomAccessRepository roomAccessRepository;

    @Mock
    private RoomAccessRepository roomAccessRepositoryMock;

    @Autowired
    private RoomAccessMapper roomAccessMapper;

    @Mock
    private RoomAccessService roomAccessServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoomAccessMockMvc;

    private RoomAccess roomAccess;

    private RoomAccess insertedRoomAccess;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomAccess createEntity() {
        return new RoomAccess().grantedAt(DEFAULT_GRANTED_AT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomAccess createUpdatedEntity() {
        return new RoomAccess().grantedAt(UPDATED_GRANTED_AT);
    }

    @BeforeEach
    void initTest() {
        roomAccess = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRoomAccess != null) {
            roomAccessRepository.delete(insertedRoomAccess);
            insertedRoomAccess = null;
        }
    }

    @Test
    @Transactional
    void createRoomAccess() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RoomAccess
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);
        var returnedRoomAccessDTO = om.readValue(
            restRoomAccessMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomAccessDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RoomAccessDTO.class
        );

        // Validate the RoomAccess in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoomAccess = roomAccessMapper.toEntity(returnedRoomAccessDTO);
        assertRoomAccessUpdatableFieldsEquals(returnedRoomAccess, getPersistedRoomAccess(returnedRoomAccess));

        insertedRoomAccess = returnedRoomAccess;
    }

    @Test
    @Transactional
    void createRoomAccessWithExistingId() throws Exception {
        // Create the RoomAccess with an existing ID
        insertedRoomAccess = roomAccessRepository.saveAndFlush(roomAccess);
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRoomAccessMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomAccessDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkGrantedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomAccess.setGrantedAt(null);

        // Create the RoomAccess, which fails.
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        restRoomAccessMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomAccessDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRoomAccesses() throws Exception {
        // Initialize the database
        insertedRoomAccess = roomAccessRepository.saveAndFlush(roomAccess);

        // Get all the roomAccessList
        restRoomAccessMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomAccess.getId().toString())))
            .andExpect(jsonPath("$.[*].grantedAt").value(hasItem(DEFAULT_GRANTED_AT.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomAccessesWithEagerRelationshipsIsEnabled() throws Exception {
        when(roomAccessServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomAccessMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(roomAccessServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomAccessesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(roomAccessServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomAccessMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(roomAccessRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRoomAccess() throws Exception {
        // Initialize the database
        insertedRoomAccess = roomAccessRepository.saveAndFlush(roomAccess);

        // Get the roomAccess
        restRoomAccessMockMvc
            .perform(get(ENTITY_API_URL_ID, roomAccess.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(roomAccess.getId().toString()))
            .andExpect(jsonPath("$.grantedAt").value(DEFAULT_GRANTED_AT.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRoomAccess() throws Exception {
        // Get the roomAccess
        restRoomAccessMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoomAccess() throws Exception {
        // Initialize the database
        insertedRoomAccess = roomAccessRepository.saveAndFlush(roomAccess);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomAccess
        RoomAccess updatedRoomAccess = roomAccessRepository.findById(roomAccess.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoomAccess are not directly saved in db
        em.detach(updatedRoomAccess);
        updatedRoomAccess.grantedAt(UPDATED_GRANTED_AT);
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(updatedRoomAccess);

        restRoomAccessMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomAccessDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomAccessDTO))
            )
            .andExpect(status().isOk());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRoomAccessToMatchAllProperties(updatedRoomAccess);
    }

    @Test
    @Transactional
    void putNonExistingRoomAccess() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomAccess.setId(UUID.randomUUID());

        // Create the RoomAccess
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomAccessMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomAccessDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomAccessDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoomAccess() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomAccess.setId(UUID.randomUUID());

        // Create the RoomAccess
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomAccessMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomAccessDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoomAccess() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomAccess.setId(UUID.randomUUID());

        // Create the RoomAccess
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomAccessMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomAccessDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRoomAccessWithPatch() throws Exception {
        // Initialize the database
        insertedRoomAccess = roomAccessRepository.saveAndFlush(roomAccess);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomAccess using partial update
        RoomAccess partialUpdatedRoomAccess = new RoomAccess();
        partialUpdatedRoomAccess.setId(roomAccess.getId());

        partialUpdatedRoomAccess.grantedAt(UPDATED_GRANTED_AT);

        restRoomAccessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomAccess.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomAccess))
            )
            .andExpect(status().isOk());

        // Validate the RoomAccess in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomAccessUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRoomAccess, roomAccess),
            getPersistedRoomAccess(roomAccess)
        );
    }

    @Test
    @Transactional
    void fullUpdateRoomAccessWithPatch() throws Exception {
        // Initialize the database
        insertedRoomAccess = roomAccessRepository.saveAndFlush(roomAccess);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomAccess using partial update
        RoomAccess partialUpdatedRoomAccess = new RoomAccess();
        partialUpdatedRoomAccess.setId(roomAccess.getId());

        partialUpdatedRoomAccess.grantedAt(UPDATED_GRANTED_AT);

        restRoomAccessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomAccess.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomAccess))
            )
            .andExpect(status().isOk());

        // Validate the RoomAccess in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomAccessUpdatableFieldsEquals(partialUpdatedRoomAccess, getPersistedRoomAccess(partialUpdatedRoomAccess));
    }

    @Test
    @Transactional
    void patchNonExistingRoomAccess() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomAccess.setId(UUID.randomUUID());

        // Create the RoomAccess
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomAccessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, roomAccessDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomAccessDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoomAccess() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomAccess.setId(UUID.randomUUID());

        // Create the RoomAccess
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomAccessMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomAccessDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoomAccess() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomAccess.setId(UUID.randomUUID());

        // Create the RoomAccess
        RoomAccessDTO roomAccessDTO = roomAccessMapper.toDto(roomAccess);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomAccessMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(roomAccessDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomAccess in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoomAccess() throws Exception {
        // Initialize the database
        insertedRoomAccess = roomAccessRepository.saveAndFlush(roomAccess);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the roomAccess
        restRoomAccessMockMvc
            .perform(delete(ENTITY_API_URL_ID, roomAccess.getId().toString()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return roomAccessRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected RoomAccess getPersistedRoomAccess(RoomAccess roomAccess) {
        return roomAccessRepository.findById(roomAccess.getId()).orElseThrow();
    }

    protected void assertPersistedRoomAccessToMatchAllProperties(RoomAccess expectedRoomAccess) {
        assertRoomAccessAllPropertiesEquals(expectedRoomAccess, getPersistedRoomAccess(expectedRoomAccess));
    }

    protected void assertPersistedRoomAccessToMatchUpdatableProperties(RoomAccess expectedRoomAccess) {
        assertRoomAccessAllUpdatablePropertiesEquals(expectedRoomAccess, getPersistedRoomAccess(expectedRoomAccess));
    }
}
