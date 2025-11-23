package com.partywave.backend.web.rest;

import static com.partywave.backend.domain.RoomInvitationAsserts.*;
import static com.partywave.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.IntegrationTest;
import com.partywave.backend.domain.RoomInvitation;
import com.partywave.backend.repository.RoomInvitationRepository;
import com.partywave.backend.service.RoomInvitationService;
import com.partywave.backend.service.dto.RoomInvitationDTO;
import com.partywave.backend.service.mapper.RoomInvitationMapper;
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
 * Integration tests for the {@link RoomInvitationResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RoomInvitationResourceIT {

    private static final String DEFAULT_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_TOKEN = "BBBBBBBBBB";

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_EXPIRES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRES_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Integer DEFAULT_MAX_USES = 1;
    private static final Integer UPDATED_MAX_USES = 2;

    private static final Integer DEFAULT_USED_COUNT = 1;
    private static final Integer UPDATED_USED_COUNT = 2;

    private static final Boolean DEFAULT_IS_ACTIVE = false;
    private static final Boolean UPDATED_IS_ACTIVE = true;

    private static final String ENTITY_API_URL = "/api/room-invitations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoomInvitationRepository roomInvitationRepository;

    @Mock
    private RoomInvitationRepository roomInvitationRepositoryMock;

    @Autowired
    private RoomInvitationMapper roomInvitationMapper;

    @Mock
    private RoomInvitationService roomInvitationServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoomInvitationMockMvc;

    private RoomInvitation roomInvitation;

    private RoomInvitation insertedRoomInvitation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomInvitation createEntity() {
        return new RoomInvitation()
            .token(DEFAULT_TOKEN)
            .createdAt(DEFAULT_CREATED_AT)
            .expiresAt(DEFAULT_EXPIRES_AT)
            .maxUses(DEFAULT_MAX_USES)
            .usedCount(DEFAULT_USED_COUNT)
            .isActive(DEFAULT_IS_ACTIVE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomInvitation createUpdatedEntity() {
        return new RoomInvitation()
            .token(UPDATED_TOKEN)
            .createdAt(UPDATED_CREATED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .maxUses(UPDATED_MAX_USES)
            .usedCount(UPDATED_USED_COUNT)
            .isActive(UPDATED_IS_ACTIVE);
    }

    @BeforeEach
    void initTest() {
        roomInvitation = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRoomInvitation != null) {
            roomInvitationRepository.delete(insertedRoomInvitation);
            insertedRoomInvitation = null;
        }
    }

    @Test
    @Transactional
    void createRoomInvitation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RoomInvitation
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);
        var returnedRoomInvitationDTO = om.readValue(
            restRoomInvitationMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomInvitationDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RoomInvitationDTO.class
        );

        // Validate the RoomInvitation in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoomInvitation = roomInvitationMapper.toEntity(returnedRoomInvitationDTO);
        assertRoomInvitationUpdatableFieldsEquals(returnedRoomInvitation, getPersistedRoomInvitation(returnedRoomInvitation));

        insertedRoomInvitation = returnedRoomInvitation;
    }

    @Test
    @Transactional
    void createRoomInvitationWithExistingId() throws Exception {
        // Create the RoomInvitation with an existing ID
        insertedRoomInvitation = roomInvitationRepository.saveAndFlush(roomInvitation);
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRoomInvitationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomInvitationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTokenIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomInvitation.setToken(null);

        // Create the RoomInvitation, which fails.
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        restRoomInvitationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomInvitationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomInvitation.setCreatedAt(null);

        // Create the RoomInvitation, which fails.
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        restRoomInvitationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomInvitationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkUsedCountIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomInvitation.setUsedCount(null);

        // Create the RoomInvitation, which fails.
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        restRoomInvitationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomInvitationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkIsActiveIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomInvitation.setIsActive(null);

        // Create the RoomInvitation, which fails.
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        restRoomInvitationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomInvitationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRoomInvitations() throws Exception {
        // Initialize the database
        insertedRoomInvitation = roomInvitationRepository.saveAndFlush(roomInvitation);

        // Get all the roomInvitationList
        restRoomInvitationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomInvitation.getId().toString())))
            .andExpect(jsonPath("$.[*].token").value(hasItem(DEFAULT_TOKEN)))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].maxUses").value(hasItem(DEFAULT_MAX_USES)))
            .andExpect(jsonPath("$.[*].usedCount").value(hasItem(DEFAULT_USED_COUNT)))
            .andExpect(jsonPath("$.[*].isActive").value(hasItem(DEFAULT_IS_ACTIVE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomInvitationsWithEagerRelationshipsIsEnabled() throws Exception {
        when(roomInvitationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomInvitationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(roomInvitationServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomInvitationsWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(roomInvitationServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomInvitationMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(roomInvitationRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRoomInvitation() throws Exception {
        // Initialize the database
        insertedRoomInvitation = roomInvitationRepository.saveAndFlush(roomInvitation);

        // Get the roomInvitation
        restRoomInvitationMockMvc
            .perform(get(ENTITY_API_URL_ID, roomInvitation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(roomInvitation.getId().toString()))
            .andExpect(jsonPath("$.token").value(DEFAULT_TOKEN))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.expiresAt").value(DEFAULT_EXPIRES_AT.toString()))
            .andExpect(jsonPath("$.maxUses").value(DEFAULT_MAX_USES))
            .andExpect(jsonPath("$.usedCount").value(DEFAULT_USED_COUNT))
            .andExpect(jsonPath("$.isActive").value(DEFAULT_IS_ACTIVE));
    }

    @Test
    @Transactional
    void getNonExistingRoomInvitation() throws Exception {
        // Get the roomInvitation
        restRoomInvitationMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoomInvitation() throws Exception {
        // Initialize the database
        insertedRoomInvitation = roomInvitationRepository.saveAndFlush(roomInvitation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomInvitation
        RoomInvitation updatedRoomInvitation = roomInvitationRepository.findById(roomInvitation.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoomInvitation are not directly saved in db
        em.detach(updatedRoomInvitation);
        updatedRoomInvitation
            .token(UPDATED_TOKEN)
            .createdAt(UPDATED_CREATED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .maxUses(UPDATED_MAX_USES)
            .usedCount(UPDATED_USED_COUNT)
            .isActive(UPDATED_IS_ACTIVE);
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(updatedRoomInvitation);

        restRoomInvitationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomInvitationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomInvitationDTO))
            )
            .andExpect(status().isOk());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRoomInvitationToMatchAllProperties(updatedRoomInvitation);
    }

    @Test
    @Transactional
    void putNonExistingRoomInvitation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomInvitation.setId(UUID.randomUUID());

        // Create the RoomInvitation
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomInvitationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomInvitationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomInvitationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoomInvitation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomInvitation.setId(UUID.randomUUID());

        // Create the RoomInvitation
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomInvitationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomInvitationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoomInvitation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomInvitation.setId(UUID.randomUUID());

        // Create the RoomInvitation
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomInvitationMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomInvitationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRoomInvitationWithPatch() throws Exception {
        // Initialize the database
        insertedRoomInvitation = roomInvitationRepository.saveAndFlush(roomInvitation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomInvitation using partial update
        RoomInvitation partialUpdatedRoomInvitation = new RoomInvitation();
        partialUpdatedRoomInvitation.setId(roomInvitation.getId());

        partialUpdatedRoomInvitation.expiresAt(UPDATED_EXPIRES_AT).maxUses(UPDATED_MAX_USES).isActive(UPDATED_IS_ACTIVE);

        restRoomInvitationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomInvitation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomInvitation))
            )
            .andExpect(status().isOk());

        // Validate the RoomInvitation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomInvitationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRoomInvitation, roomInvitation),
            getPersistedRoomInvitation(roomInvitation)
        );
    }

    @Test
    @Transactional
    void fullUpdateRoomInvitationWithPatch() throws Exception {
        // Initialize the database
        insertedRoomInvitation = roomInvitationRepository.saveAndFlush(roomInvitation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomInvitation using partial update
        RoomInvitation partialUpdatedRoomInvitation = new RoomInvitation();
        partialUpdatedRoomInvitation.setId(roomInvitation.getId());

        partialUpdatedRoomInvitation
            .token(UPDATED_TOKEN)
            .createdAt(UPDATED_CREATED_AT)
            .expiresAt(UPDATED_EXPIRES_AT)
            .maxUses(UPDATED_MAX_USES)
            .usedCount(UPDATED_USED_COUNT)
            .isActive(UPDATED_IS_ACTIVE);

        restRoomInvitationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomInvitation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomInvitation))
            )
            .andExpect(status().isOk());

        // Validate the RoomInvitation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomInvitationUpdatableFieldsEquals(partialUpdatedRoomInvitation, getPersistedRoomInvitation(partialUpdatedRoomInvitation));
    }

    @Test
    @Transactional
    void patchNonExistingRoomInvitation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomInvitation.setId(UUID.randomUUID());

        // Create the RoomInvitation
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomInvitationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, roomInvitationDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomInvitationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoomInvitation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomInvitation.setId(UUID.randomUUID());

        // Create the RoomInvitation
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomInvitationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomInvitationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoomInvitation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomInvitation.setId(UUID.randomUUID());

        // Create the RoomInvitation
        RoomInvitationDTO roomInvitationDTO = roomInvitationMapper.toDto(roomInvitation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomInvitationMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(roomInvitationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomInvitation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoomInvitation() throws Exception {
        // Initialize the database
        insertedRoomInvitation = roomInvitationRepository.saveAndFlush(roomInvitation);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the roomInvitation
        restRoomInvitationMockMvc
            .perform(delete(ENTITY_API_URL_ID, roomInvitation.getId().toString()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return roomInvitationRepository.count();
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

    protected RoomInvitation getPersistedRoomInvitation(RoomInvitation roomInvitation) {
        return roomInvitationRepository.findById(roomInvitation.getId()).orElseThrow();
    }

    protected void assertPersistedRoomInvitationToMatchAllProperties(RoomInvitation expectedRoomInvitation) {
        assertRoomInvitationAllPropertiesEquals(expectedRoomInvitation, getPersistedRoomInvitation(expectedRoomInvitation));
    }

    protected void assertPersistedRoomInvitationToMatchUpdatableProperties(RoomInvitation expectedRoomInvitation) {
        assertRoomInvitationAllUpdatablePropertiesEquals(expectedRoomInvitation, getPersistedRoomInvitation(expectedRoomInvitation));
    }
}
