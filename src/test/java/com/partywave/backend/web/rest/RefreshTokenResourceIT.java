package com.partywave.backend.web.rest;

import static com.partywave.backend.domain.RefreshTokenAsserts.*;
import static com.partywave.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.IntegrationTest;
import com.partywave.backend.domain.RefreshToken;
import com.partywave.backend.repository.RefreshTokenRepository;
import com.partywave.backend.service.RefreshTokenService;
import com.partywave.backend.service.dto.RefreshTokenDTO;
import com.partywave.backend.service.mapper.RefreshTokenMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
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
 * Integration tests for the {@link RefreshTokenResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class RefreshTokenResourceIT {

    private static final String DEFAULT_TOKEN_HASH = "AAAAAAAAAA";
    private static final String UPDATED_TOKEN_HASH = "BBBBBBBBBB";

    private static final Instant DEFAULT_EXPIRES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRES_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_CREATED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_REVOKED_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_REVOKED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_DEVICE_INFO = "AAAAAAAAAA";
    private static final String UPDATED_DEVICE_INFO = "BBBBBBBBBB";

    private static final String DEFAULT_IP_ADDRESS = "AAAAAAAAAA";
    private static final String UPDATED_IP_ADDRESS = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/refresh-tokens";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepositoryMock;

    @Autowired
    private RefreshTokenMapper refreshTokenMapper;

    @Mock
    private RefreshTokenService refreshTokenServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restRefreshTokenMockMvc;

    private RefreshToken refreshToken;

    private RefreshToken insertedRefreshToken;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RefreshToken createEntity() {
        return new RefreshToken()
            .tokenHash(DEFAULT_TOKEN_HASH)
            .expiresAt(DEFAULT_EXPIRES_AT)
            .createdAt(DEFAULT_CREATED_AT)
            .revokedAt(DEFAULT_REVOKED_AT)
            .deviceInfo(DEFAULT_DEVICE_INFO)
            .ipAddress(DEFAULT_IP_ADDRESS);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static RefreshToken createUpdatedEntity() {
        return new RefreshToken()
            .tokenHash(UPDATED_TOKEN_HASH)
            .expiresAt(UPDATED_EXPIRES_AT)
            .createdAt(UPDATED_CREATED_AT)
            .revokedAt(UPDATED_REVOKED_AT)
            .deviceInfo(UPDATED_DEVICE_INFO)
            .ipAddress(UPDATED_IP_ADDRESS);
    }

    @BeforeEach
    void initTest() {
        refreshToken = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedRefreshToken != null) {
            refreshTokenRepository.delete(insertedRefreshToken);
            insertedRefreshToken = null;
        }
    }

    @Test
    @Transactional
    void createRefreshToken() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the RefreshToken
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);
        var returnedRefreshTokenDTO = om.readValue(
            restRefreshTokenMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshTokenDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            RefreshTokenDTO.class
        );

        // Validate the RefreshToken in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedRefreshToken = refreshTokenMapper.toEntity(returnedRefreshTokenDTO);
        assertRefreshTokenUpdatableFieldsEquals(returnedRefreshToken, getPersistedRefreshToken(returnedRefreshToken));

        insertedRefreshToken = returnedRefreshToken;
    }

    @Test
    @Transactional
    void createRefreshTokenWithExistingId() throws Exception {
        // Create the RefreshToken with an existing ID
        refreshToken.setId(1L);
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restRefreshTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshTokenDTO)))
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTokenHashIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        refreshToken.setTokenHash(null);

        // Create the RefreshToken, which fails.
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        restRefreshTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshTokenDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkExpiresAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        refreshToken.setExpiresAt(null);

        // Create the RefreshToken, which fails.
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        restRefreshTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshTokenDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkCreatedAtIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        refreshToken.setCreatedAt(null);

        // Create the RefreshToken, which fails.
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        restRefreshTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshTokenDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllRefreshTokens() throws Exception {
        // Initialize the database
        insertedRefreshToken = refreshTokenRepository.saveAndFlush(refreshToken);

        // Get all the refreshTokenList
        restRefreshTokenMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(refreshToken.getId().intValue())))
            .andExpect(jsonPath("$.[*].tokenHash").value(hasItem(DEFAULT_TOKEN_HASH)))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].createdAt").value(hasItem(DEFAULT_CREATED_AT.toString())))
            .andExpect(jsonPath("$.[*].revokedAt").value(hasItem(DEFAULT_REVOKED_AT.toString())))
            .andExpect(jsonPath("$.[*].deviceInfo").value(hasItem(DEFAULT_DEVICE_INFO)))
            .andExpect(jsonPath("$.[*].ipAddress").value(hasItem(DEFAULT_IP_ADDRESS)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRefreshTokensWithEagerRelationshipsIsEnabled() throws Exception {
        when(refreshTokenServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRefreshTokenMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(refreshTokenServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllRefreshTokensWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(refreshTokenServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restRefreshTokenMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(refreshTokenRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getRefreshToken() throws Exception {
        // Initialize the database
        insertedRefreshToken = refreshTokenRepository.saveAndFlush(refreshToken);

        // Get the refreshToken
        restRefreshTokenMockMvc
            .perform(get(ENTITY_API_URL_ID, refreshToken.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(refreshToken.getId().intValue()))
            .andExpect(jsonPath("$.tokenHash").value(DEFAULT_TOKEN_HASH))
            .andExpect(jsonPath("$.expiresAt").value(DEFAULT_EXPIRES_AT.toString()))
            .andExpect(jsonPath("$.createdAt").value(DEFAULT_CREATED_AT.toString()))
            .andExpect(jsonPath("$.revokedAt").value(DEFAULT_REVOKED_AT.toString()))
            .andExpect(jsonPath("$.deviceInfo").value(DEFAULT_DEVICE_INFO))
            .andExpect(jsonPath("$.ipAddress").value(DEFAULT_IP_ADDRESS));
    }

    @Test
    @Transactional
    void getNonExistingRefreshToken() throws Exception {
        // Get the refreshToken
        restRefreshTokenMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingRefreshToken() throws Exception {
        // Initialize the database
        insertedRefreshToken = refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the refreshToken
        RefreshToken updatedRefreshToken = refreshTokenRepository.findById(refreshToken.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedRefreshToken are not directly saved in db
        em.detach(updatedRefreshToken);
        updatedRefreshToken
            .tokenHash(UPDATED_TOKEN_HASH)
            .expiresAt(UPDATED_EXPIRES_AT)
            .createdAt(UPDATED_CREATED_AT)
            .revokedAt(UPDATED_REVOKED_AT)
            .deviceInfo(UPDATED_DEVICE_INFO)
            .ipAddress(UPDATED_IP_ADDRESS);
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(updatedRefreshToken);

        restRefreshTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, refreshTokenDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(refreshTokenDTO))
            )
            .andExpect(status().isOk());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedRefreshTokenToMatchAllProperties(updatedRefreshToken);
    }

    @Test
    @Transactional
    void putNonExistingRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // Create the RefreshToken
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, refreshTokenDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(refreshTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // Create the RefreshToken
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(refreshTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // Create the RefreshToken
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(refreshTokenDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateRefreshTokenWithPatch() throws Exception {
        // Initialize the database
        insertedRefreshToken = refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the refreshToken using partial update
        RefreshToken partialUpdatedRefreshToken = new RefreshToken();
        partialUpdatedRefreshToken.setId(refreshToken.getId());

        partialUpdatedRefreshToken.expiresAt(UPDATED_EXPIRES_AT).deviceInfo(UPDATED_DEVICE_INFO).ipAddress(UPDATED_IP_ADDRESS);

        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRefreshToken.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRefreshToken))
            )
            .andExpect(status().isOk());

        // Validate the RefreshToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRefreshTokenUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedRefreshToken, refreshToken),
            getPersistedRefreshToken(refreshToken)
        );
    }

    @Test
    @Transactional
    void fullUpdateRefreshTokenWithPatch() throws Exception {
        // Initialize the database
        insertedRefreshToken = refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the refreshToken using partial update
        RefreshToken partialUpdatedRefreshToken = new RefreshToken();
        partialUpdatedRefreshToken.setId(refreshToken.getId());

        partialUpdatedRefreshToken
            .tokenHash(UPDATED_TOKEN_HASH)
            .expiresAt(UPDATED_EXPIRES_AT)
            .createdAt(UPDATED_CREATED_AT)
            .revokedAt(UPDATED_REVOKED_AT)
            .deviceInfo(UPDATED_DEVICE_INFO)
            .ipAddress(UPDATED_IP_ADDRESS);

        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedRefreshToken.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedRefreshToken))
            )
            .andExpect(status().isOk());

        // Validate the RefreshToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertRefreshTokenUpdatableFieldsEquals(partialUpdatedRefreshToken, getPersistedRefreshToken(partialUpdatedRefreshToken));
    }

    @Test
    @Transactional
    void patchNonExistingRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // Create the RefreshToken
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, refreshTokenDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(refreshTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // Create the RefreshToken
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(refreshTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamRefreshToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        refreshToken.setId(longCount.incrementAndGet());

        // Create the RefreshToken
        RefreshTokenDTO refreshTokenDTO = refreshTokenMapper.toDto(refreshToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restRefreshTokenMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(refreshTokenDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the RefreshToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteRefreshToken() throws Exception {
        // Initialize the database
        insertedRefreshToken = refreshTokenRepository.saveAndFlush(refreshToken);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the refreshToken
        restRefreshTokenMockMvc
            .perform(delete(ENTITY_API_URL_ID, refreshToken.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return refreshTokenRepository.count();
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

    protected RefreshToken getPersistedRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.findById(refreshToken.getId()).orElseThrow();
    }

    protected void assertPersistedRefreshTokenToMatchAllProperties(RefreshToken expectedRefreshToken) {
        assertRefreshTokenAllPropertiesEquals(expectedRefreshToken, getPersistedRefreshToken(expectedRefreshToken));
    }

    protected void assertPersistedRefreshTokenToMatchUpdatableProperties(RefreshToken expectedRefreshToken) {
        assertRefreshTokenAllUpdatablePropertiesEquals(expectedRefreshToken, getPersistedRefreshToken(expectedRefreshToken));
    }
}
