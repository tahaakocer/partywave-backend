package com.partywave.backend.web.rest;

import static com.partywave.backend.domain.UserTokenAsserts.*;
import static com.partywave.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.IntegrationTest;
import com.partywave.backend.domain.AppUser;
import com.partywave.backend.domain.UserToken;
import com.partywave.backend.repository.UserTokenRepository;
import com.partywave.backend.service.UserTokenService;
import com.partywave.backend.service.dto.UserTokenDTO;
import com.partywave.backend.service.mapper.UserTokenMapper;
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
 * Integration tests for the {@link UserTokenResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class UserTokenResourceIT {

    private static final String DEFAULT_ACCESS_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_ACCESS_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_REFRESH_TOKEN = "AAAAAAAAAA";
    private static final String UPDATED_REFRESH_TOKEN = "BBBBBBBBBB";

    private static final String DEFAULT_TOKEN_TYPE = "AAAAAAAAAA";
    private static final String UPDATED_TOKEN_TYPE = "BBBBBBBBBB";

    private static final Instant DEFAULT_EXPIRES_AT = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_EXPIRES_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_SCOPE = "AAAAAAAAAA";
    private static final String UPDATED_SCOPE = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/user-tokens";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    @Autowired
    private ObjectMapper om;

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Mock
    private UserTokenRepository userTokenRepositoryMock;

    @Autowired
    private UserTokenMapper userTokenMapper;

    @Mock
    private UserTokenService userTokenServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restUserTokenMockMvc;

    private UserToken userToken;

    private UserToken insertedUserToken;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserToken createEntity(EntityManager em) {
        UserToken userToken = new UserToken()
            .accessToken(DEFAULT_ACCESS_TOKEN)
            .refreshToken(DEFAULT_REFRESH_TOKEN)
            .tokenType(DEFAULT_TOKEN_TYPE)
            .expiresAt(DEFAULT_EXPIRES_AT)
            .scope(DEFAULT_SCOPE);
        // Add required entity
        AppUser appUser;
        if (TestUtil.findAll(em, AppUser.class).isEmpty()) {
            appUser = AppUserResourceIT.createEntity();
            em.persist(appUser);
            em.flush();
        } else {
            appUser = TestUtil.findAll(em, AppUser.class).get(0);
        }
        userToken.setAppUser(appUser);
        return userToken;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static UserToken createUpdatedEntity(EntityManager em) {
        UserToken updatedUserToken = new UserToken()
            .accessToken(UPDATED_ACCESS_TOKEN)
            .refreshToken(UPDATED_REFRESH_TOKEN)
            .tokenType(UPDATED_TOKEN_TYPE)
            .expiresAt(UPDATED_EXPIRES_AT)
            .scope(UPDATED_SCOPE);
        // Add required entity
        AppUser appUser;
        if (TestUtil.findAll(em, AppUser.class).isEmpty()) {
            appUser = AppUserResourceIT.createUpdatedEntity();
            em.persist(appUser);
            em.flush();
        } else {
            appUser = TestUtil.findAll(em, AppUser.class).get(0);
        }
        updatedUserToken.setAppUser(appUser);
        return updatedUserToken;
    }

    @BeforeEach
    void initTest() {
        userToken = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedUserToken != null) {
            userTokenRepository.delete(insertedUserToken);
            insertedUserToken = null;
        }
    }

    @Test
    @Transactional
    void createUserToken() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the UserToken
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);
        var returnedUserTokenDTO = om.readValue(
            restUserTokenMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            UserTokenDTO.class
        );

        // Validate the UserToken in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedUserToken = userTokenMapper.toEntity(returnedUserTokenDTO);
        assertUserTokenUpdatableFieldsEquals(returnedUserToken, getPersistedUserToken(returnedUserToken));

        insertedUserToken = returnedUserToken;
    }

    @Test
    @Transactional
    void createUserTokenWithExistingId() throws Exception {
        // Create the UserToken with an existing ID
        insertedUserToken = userTokenRepository.saveAndFlush(userToken);
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restUserTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenDTO)))
            .andExpect(status().isBadRequest());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkAccessTokenIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        userToken.setAccessToken(null);

        // Create the UserToken, which fails.
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        restUserTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkRefreshTokenIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        userToken.setRefreshToken(null);

        // Create the UserToken, which fails.
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        restUserTokenMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllUserTokens() throws Exception {
        // Initialize the database
        insertedUserToken = userTokenRepository.saveAndFlush(userToken);

        // Get all the userTokenList
        restUserTokenMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(userToken.getId().toString())))
            .andExpect(jsonPath("$.[*].accessToken").value(hasItem(DEFAULT_ACCESS_TOKEN)))
            .andExpect(jsonPath("$.[*].refreshToken").value(hasItem(DEFAULT_REFRESH_TOKEN)))
            .andExpect(jsonPath("$.[*].tokenType").value(hasItem(DEFAULT_TOKEN_TYPE)))
            .andExpect(jsonPath("$.[*].expiresAt").value(hasItem(DEFAULT_EXPIRES_AT.toString())))
            .andExpect(jsonPath("$.[*].scope").value(hasItem(DEFAULT_SCOPE)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllUserTokensWithEagerRelationshipsIsEnabled() throws Exception {
        when(userTokenServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restUserTokenMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(userTokenServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllUserTokensWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(userTokenServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restUserTokenMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(userTokenRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getUserToken() throws Exception {
        // Initialize the database
        insertedUserToken = userTokenRepository.saveAndFlush(userToken);

        // Get the userToken
        restUserTokenMockMvc
            .perform(get(ENTITY_API_URL_ID, userToken.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(userToken.getId().toString()))
            .andExpect(jsonPath("$.accessToken").value(DEFAULT_ACCESS_TOKEN))
            .andExpect(jsonPath("$.refreshToken").value(DEFAULT_REFRESH_TOKEN))
            .andExpect(jsonPath("$.tokenType").value(DEFAULT_TOKEN_TYPE))
            .andExpect(jsonPath("$.expiresAt").value(DEFAULT_EXPIRES_AT.toString()))
            .andExpect(jsonPath("$.scope").value(DEFAULT_SCOPE));
    }

    @Test
    @Transactional
    void getNonExistingUserToken() throws Exception {
        // Get the userToken
        restUserTokenMockMvc.perform(get(ENTITY_API_URL_ID, UUID.randomUUID().toString())).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingUserToken() throws Exception {
        // Initialize the database
        insertedUserToken = userTokenRepository.saveAndFlush(userToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userToken
        UserToken updatedUserToken = userTokenRepository.findById(userToken.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedUserToken are not directly saved in db
        em.detach(updatedUserToken);
        updatedUserToken
            .accessToken(UPDATED_ACCESS_TOKEN)
            .refreshToken(UPDATED_REFRESH_TOKEN)
            .tokenType(UPDATED_TOKEN_TYPE)
            .expiresAt(UPDATED_EXPIRES_AT)
            .scope(UPDATED_SCOPE);
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(updatedUserToken);

        restUserTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, userTokenDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userTokenDTO))
            )
            .andExpect(status().isOk());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedUserTokenToMatchAllProperties(updatedUserToken);
    }

    @Test
    @Transactional
    void putNonExistingUserToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userToken.setId(UUID.randomUUID());

        // Create the UserToken
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, userTokenDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchUserToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userToken.setId(UUID.randomUUID());

        // Create the UserToken
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenMockMvc
            .perform(
                put(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(userTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamUserToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userToken.setId(UUID.randomUUID());

        // Create the UserToken
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(userTokenDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateUserTokenWithPatch() throws Exception {
        // Initialize the database
        insertedUserToken = userTokenRepository.saveAndFlush(userToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userToken using partial update
        UserToken partialUpdatedUserToken = new UserToken();
        partialUpdatedUserToken.setId(userToken.getId());

        partialUpdatedUserToken.refreshToken(UPDATED_REFRESH_TOKEN).tokenType(UPDATED_TOKEN_TYPE).scope(UPDATED_SCOPE);

        restUserTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserToken.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUserToken))
            )
            .andExpect(status().isOk());

        // Validate the UserToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUserTokenUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedUserToken, userToken),
            getPersistedUserToken(userToken)
        );
    }

    @Test
    @Transactional
    void fullUpdateUserTokenWithPatch() throws Exception {
        // Initialize the database
        insertedUserToken = userTokenRepository.saveAndFlush(userToken);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the userToken using partial update
        UserToken partialUpdatedUserToken = new UserToken();
        partialUpdatedUserToken.setId(userToken.getId());

        partialUpdatedUserToken
            .accessToken(UPDATED_ACCESS_TOKEN)
            .refreshToken(UPDATED_REFRESH_TOKEN)
            .tokenType(UPDATED_TOKEN_TYPE)
            .expiresAt(UPDATED_EXPIRES_AT)
            .scope(UPDATED_SCOPE);

        restUserTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedUserToken.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedUserToken))
            )
            .andExpect(status().isOk());

        // Validate the UserToken in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertUserTokenUpdatableFieldsEquals(partialUpdatedUserToken, getPersistedUserToken(partialUpdatedUserToken));
    }

    @Test
    @Transactional
    void patchNonExistingUserToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userToken.setId(UUID.randomUUID());

        // Create the UserToken
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restUserTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, userTokenDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(userTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchUserToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userToken.setId(UUID.randomUUID());

        // Create the UserToken
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, UUID.randomUUID())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(userTokenDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamUserToken() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        userToken.setId(UUID.randomUUID());

        // Create the UserToken
        UserTokenDTO userTokenDTO = userTokenMapper.toDto(userToken);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restUserTokenMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(userTokenDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the UserToken in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteUserToken() throws Exception {
        // Initialize the database
        insertedUserToken = userTokenRepository.saveAndFlush(userToken);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the userToken
        restUserTokenMockMvc
            .perform(delete(ENTITY_API_URL_ID, userToken.getId().toString()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return userTokenRepository.count();
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

    protected UserToken getPersistedUserToken(UserToken userToken) {
        return userTokenRepository.findById(userToken.getId()).orElseThrow();
    }

    protected void assertPersistedUserTokenToMatchAllProperties(UserToken expectedUserToken) {
        assertUserTokenAllPropertiesEquals(expectedUserToken, getPersistedUserToken(expectedUserToken));
    }

    protected void assertPersistedUserTokenToMatchUpdatableProperties(UserToken expectedUserToken) {
        assertUserTokenAllUpdatablePropertiesEquals(expectedUserToken, getPersistedUserToken(expectedUserToken));
    }
}
