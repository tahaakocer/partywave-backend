package com.partywave.backend.web.rest;

import static com.partywave.backend.domain.RoomMemberAsserts.*;
import static com.partywave.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.IntegrationTest;
import com.partywave.backend.domain.RoomMember;
import com.partywave.backend.domain.enumeration.RoomMemberRole;
import com.partywave.backend.repository.RoomMemberRepository;
import com.partywave.backend.service.RoomMemberService;
import com.partywave.backend.service.dto.RoomMemberDTO;
import com.partywave.backend.service.mapper.RoomMemberMapper;
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
 * Integration tests for the {@link RoomMemberResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RoomMemberResourceIT {

    private static final Instant DEFAULT_JOINED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_JOINED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_LAST_ACTIVE_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_LAST_ACTIVE_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final RoomMemberRole DEFAULT_ROLE = RoomMemberRole.OWNER;
    private static final RoomMemberRole UPDATED_ROLE = RoomMemberRole.DJ;

    private static final String ENTITY_API_URL = "/api/room-members";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private RoomMemberRepository roomMemberRepositoryMock;

    @Autowired
    private RoomMemberMapper roomMemberMapper;

    @Mock
    private RoomMemberService roomMemberServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRoomMemberMockMvc;

    private RoomMember roomMember;

    private RoomMember insertedRoomMember;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomMember createEntity() {
        return new RoomMember().joinedAt(DEFAULT_JOINED_AT).lastActiveAt(DEFAULT_LAST_ACTIVE_AT).role(DEFAULT_ROLE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RoomMember createUpdatedEntity() {
        return new RoomMember().joinedAt(UPDATED_JOINED_AT).lastActiveAt(UPDATED_LAST_ACTIVE_AT).role(UPDATED_ROLE);
    }

    @BeforeEach
    void initTest() {
        roomMember = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRoomMember != null) {
            roomMemberRepository.delete(insertedRoomMember);
            insertedRoomMember = null;
        }
    }

    @Test
    @Transactional
    void createRoomMember() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);
        var returnedRoomMemberDTO = om.readValue(
            restRoomMemberMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RoomMemberDTO.class
        );

        // Validate the RoomMember in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRoomMember = roomMemberMapper.toEntity(returnedRoomMemberDTO);
        assertRoomMemberUpdatableFieldsEquals(returnedRoomMember, getPersistedRoomMember(returnedRoomMember));

        insertedRoomMember = returnedRoomMember;
    }

    @Test
    @Transactional
    void createRoomMemberWithExistingId() throws Exception {
        // Create the RoomMember with an existing ID
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRoomMemberMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkJoinedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomMember.setJoinedAt(null);

        // Create the RoomMember, which fails.
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        restRoomMemberMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRoleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        roomMember.setRole(null);

        // Create the RoomMember, which fails.
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        restRoomMemberMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRoomMembers() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get all the roomMemberList
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(roomMember.getId().toString())))
            .andExpect(jsonPath("$.[*].joinedAt").value(hasItem(DEFAULT_JOINED_AT.toString())))
            .andExpect(jsonPath("$.[*].lastActiveAt").value(hasItem(DEFAULT_LAST_ACTIVE_AT.toString())))
            .andExpect(jsonPath("$.[*].role").value(hasItem(DEFAULT_ROLE.toString())));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomMembersWithEagerRelationshipsIsEnabled() throws Exception {
        when(roomMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(roomMemberServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRoomMembersWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(roomMemberServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRoomMemberMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(roomMemberRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRoomMember() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        // Get the roomMember
        restRoomMemberMockMvc
            .perform(get(ENTITY_API_URL_ID, roomMember.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(roomMember.getId().toString()))
            .andExpect(jsonPath("$.joinedAt").value(DEFAULT_JOINED_AT.toString()))
            .andExpect(jsonPath("$.lastActiveAt").value(DEFAULT_LAST_ACTIVE_AT.toString()))
            .andExpect(jsonPath("$.role").value(DEFAULT_ROLE.toString()));
    }

    @Test
    @Transactional
    void getNonExistingRoomMember() throws Exception {
        // Get the roomMember
        restRoomMemberMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRoomMember() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomMember
        RoomMember updatedRoomMember = roomMemberRepository.findById(roomMember.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRoomMember are not directly saved in db
        em.detach(updatedRoomMember);
        updatedRoomMember.joinedAt(UPDATED_JOINED_AT).lastActiveAt(UPDATED_LAST_ACTIVE_AT).role(UPDATED_ROLE);
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(updatedRoomMember);

        restRoomMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomMemberDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isOk());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRoomMemberToMatchAllProperties(updatedRoomMember);
    }

    @Test
    @Transactional
    void putNonExistingRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, roomMemberDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRoomMemberWithPatch() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomMember using partial update
        RoomMember partialUpdatedRoomMember = new RoomMember();
        partialUpdatedRoomMember.setId(roomMember.getId());

        partialUpdatedRoomMember.joinedAt(UPDATED_JOINED_AT);

        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomMember.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomMember))
            )
            .andExpect(status().isOk());

        // Validate the RoomMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomMemberUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRoomMember, roomMember),
            getPersistedRoomMember(roomMember)
        );
    }

    @Test
    @Transactional
    void fullUpdateRoomMemberWithPatch() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the roomMember using partial update
        RoomMember partialUpdatedRoomMember = new RoomMember();
        partialUpdatedRoomMember.setId(roomMember.getId());

        partialUpdatedRoomMember.joinedAt(UPDATED_JOINED_AT).lastActiveAt(UPDATED_LAST_ACTIVE_AT).role(UPDATED_ROLE);

        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRoomMember.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRoomMember))
            )
            .andExpect(status().isOk());

        // Validate the RoomMember in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRoomMemberUpdatableFieldsEquals(partialUpdatedRoomMember, getPersistedRoomMember(partialUpdatedRoomMember));
    }

    @Test
    @Transactional
    void patchNonExistingRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, roomMemberDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(roomMemberDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRoomMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        roomMember.setId(UUID.randomUUID());

        // Create the RoomMember
        RoomMemberDTO roomMemberDTO = roomMemberMapper.toDto(roomMember);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRoomMemberMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(roomMemberDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RoomMember in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRoomMember() throws Exception {
        // Initialize the database
        insertedRoomMember = roomMemberRepository.saveAndFlush(roomMember);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the roomMember
        restRoomMemberMockMvc
            .perform(delete(ENTITY_API_URL_ID, roomMember.getId().toString()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return roomMemberRepository.count();
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

    protected RoomMember getPersistedRoomMember(RoomMember roomMember) {
        return roomMemberRepository.findById(roomMember.getId()).orElseThrow();
    }

    protected void assertPersistedRoomMemberToMatchAllProperties(RoomMember expectedRoomMember) {
        assertRoomMemberAllPropertiesEquals(expectedRoomMember, getPersistedRoomMember(expectedRoomMember));
    }

    protected void assertPersistedRoomMemberToMatchUpdatableProperties(RoomMember expectedRoomMember) {
        assertRoomMemberAllUpdatablePropertiesEquals(expectedRoomMember, getPersistedRoomMember(expectedRoomMember));
    }
}
