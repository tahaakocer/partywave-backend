package com.partywave.backend.web.rest;

import static com.partywave.backend.domain.AppUserStatsAsserts.*;
import static com.partywave.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.IntegrationTest;
import com.partywave.backend.domain.AppUserStats;
import com.partywave.backend.repository.AppUserStatsRepository;
import com.partywave.backend.service.dto.AppUserStatsDTO;
import com.partywave.backend.service.mapper.AppUserStatsMapper;
import jakarta.persistence.EntityManager;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link AppUserStatsResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class AppUserStatsResourceIT {

    private static final Integer DEFAULT_TOTAL_LIKE = 1;
    private static final Integer UPDATED_TOTAL_LIKE = 2;

    private static final Integer DEFAULT_TOTAL_DISLIKE = 1;
    private static final Integer UPDATED_TOTAL_DISLIKE = 2;

    private static final String ENTITY_API_URL = "/api/app-user-stats";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AppUserStatsRepository appUserStatsRepository;

    @Autowired
    private AppUserStatsMapper appUserStatsMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAppUserStatsMockMvc;

    private AppUserStats appUserStats;

    private AppUserStats insertedAppUserStats;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AppUserStats createEntity() {
        return new AppUserStats().totalLike(DEFAULT_TOTAL_LIKE).totalDislike(DEFAULT_TOTAL_DISLIKE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AppUserStats createUpdatedEntity() {
        return new AppUserStats().totalLike(UPDATED_TOTAL_LIKE).totalDislike(UPDATED_TOTAL_DISLIKE);
    }

    @BeforeEach
    void initTest() {
        appUserStats = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAppUserStats != null) {
            appUserStatsRepository.delete(insertedAppUserStats);
            insertedAppUserStats = null;
        }
    }

    @Test
    @Transactional
    void createAppUserStats() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AppUserStats
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);
        var returnedAppUserStatsDTO = om.readValue(
            restAppUserStatsMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(appUserStatsDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AppUserStatsDTO.class
        );

        // Validate the AppUserStats in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAppUserStats = appUserStatsMapper.toEntity(returnedAppUserStatsDTO);
        assertAppUserStatsUpdatableFieldsEquals(returnedAppUserStats, getPersistedAppUserStats(returnedAppUserStats));

        insertedAppUserStats = returnedAppUserStats;
    }

    @Test
    @Transactional
    void createAppUserStatsWithExistingId() throws Exception {
        // Create the AppUserStats with an existing ID
        appUserStats.setId(1L);
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAppUserStatsMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(appUserStatsDTO)))
            .andExpect(status().isBadRequest());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllAppUserStats() throws Exception {
        // Initialize the database
        insertedAppUserStats = appUserStatsRepository.saveAndFlush(appUserStats);

        // Get all the appUserStatsList
        restAppUserStatsMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(appUserStats.getId().intValue())))
            .andExpect(jsonPath("$.[*].totalLike").value(hasItem(DEFAULT_TOTAL_LIKE)))
            .andExpect(jsonPath("$.[*].totalDislike").value(hasItem(DEFAULT_TOTAL_DISLIKE)));
    }

    @Test
    @Transactional
    void getAppUserStats() throws Exception {
        // Initialize the database
        insertedAppUserStats = appUserStatsRepository.saveAndFlush(appUserStats);

        // Get the appUserStats
        restAppUserStatsMockMvc
            .perform(get(ENTITY_API_URL_ID, appUserStats.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(appUserStats.getId().intValue()))
            .andExpect(jsonPath("$.totalLike").value(DEFAULT_TOTAL_LIKE))
            .andExpect(jsonPath("$.totalDislike").value(DEFAULT_TOTAL_DISLIKE));
    }

    @Test
    @Transactional
    void getNonExistingAppUserStats() throws Exception {
        // Get the appUserStats
        restAppUserStatsMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAppUserStats() throws Exception {
        // Initialize the database
        insertedAppUserStats = appUserStatsRepository.saveAndFlush(appUserStats);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the appUserStats
        AppUserStats updatedAppUserStats = appUserStatsRepository.findById(appUserStats.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAppUserStats are not directly saved in db
        em.detach(updatedAppUserStats);
        updatedAppUserStats.totalLike(UPDATED_TOTAL_LIKE).totalDislike(UPDATED_TOTAL_DISLIKE);
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(updatedAppUserStats);

        restAppUserStatsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, appUserStatsDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(appUserStatsDTO))
            )
            .andExpect(status().isOk());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAppUserStatsToMatchAllProperties(updatedAppUserStats);
    }

    @Test
    @Transactional
    void putNonExistingAppUserStats() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserStats.setId(longCount.incrementAndGet());

        // Create the AppUserStats
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAppUserStatsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, appUserStatsDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(appUserStatsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAppUserStats() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserStats.setId(longCount.incrementAndGet());

        // Create the AppUserStats
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserStatsMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(appUserStatsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAppUserStats() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserStats.setId(longCount.incrementAndGet());

        // Create the AppUserStats
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserStatsMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(appUserStatsDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAppUserStatsWithPatch() throws Exception {
        // Initialize the database
        insertedAppUserStats = appUserStatsRepository.saveAndFlush(appUserStats);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the appUserStats using partial update
        AppUserStats partialUpdatedAppUserStats = new AppUserStats();
        partialUpdatedAppUserStats.setId(appUserStats.getId());

        partialUpdatedAppUserStats.totalDislike(UPDATED_TOTAL_DISLIKE);

        restAppUserStatsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAppUserStats.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAppUserStats))
            )
            .andExpect(status().isOk());

        // Validate the AppUserStats in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAppUserStatsUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAppUserStats, appUserStats),
            getPersistedAppUserStats(appUserStats)
        );
    }

    @Test
    @Transactional
    void fullUpdateAppUserStatsWithPatch() throws Exception {
        // Initialize the database
        insertedAppUserStats = appUserStatsRepository.saveAndFlush(appUserStats);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the appUserStats using partial update
        AppUserStats partialUpdatedAppUserStats = new AppUserStats();
        partialUpdatedAppUserStats.setId(appUserStats.getId());

        partialUpdatedAppUserStats.totalLike(UPDATED_TOTAL_LIKE).totalDislike(UPDATED_TOTAL_DISLIKE);

        restAppUserStatsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAppUserStats.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAppUserStats))
            )
            .andExpect(status().isOk());

        // Validate the AppUserStats in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAppUserStatsUpdatableFieldsEquals(partialUpdatedAppUserStats, getPersistedAppUserStats(partialUpdatedAppUserStats));
    }

    @Test
    @Transactional
    void patchNonExistingAppUserStats() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserStats.setId(longCount.incrementAndGet());

        // Create the AppUserStats
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAppUserStatsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, appUserStatsDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(appUserStatsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAppUserStats() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserStats.setId(longCount.incrementAndGet());

        // Create the AppUserStats
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserStatsMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(appUserStatsDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAppUserStats() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserStats.setId(longCount.incrementAndGet());

        // Create the AppUserStats
        AppUserStatsDTO appUserStatsDTO = appUserStatsMapper.toDto(appUserStats);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserStatsMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(appUserStatsDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AppUserStats in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAppUserStats() throws Exception {
        // Initialize the database
        insertedAppUserStats = appUserStatsRepository.saveAndFlush(appUserStats);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the appUserStats
        restAppUserStatsMockMvc
            .perform(delete(ENTITY_API_URL_ID, appUserStats.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return appUserStatsRepository.count();
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

    protected AppUserStats getPersistedAppUserStats(AppUserStats appUserStats) {
        return appUserStatsRepository.findById(appUserStats.getId()).orElseThrow();
    }

    protected void assertPersistedAppUserStatsToMatchAllProperties(AppUserStats expectedAppUserStats) {
        assertAppUserStatsAllPropertiesEquals(expectedAppUserStats, getPersistedAppUserStats(expectedAppUserStats));
    }

    protected void assertPersistedAppUserStatsToMatchUpdatableProperties(AppUserStats expectedAppUserStats) {
        assertAppUserStatsAllUpdatablePropertiesEquals(expectedAppUserStats, getPersistedAppUserStats(expectedAppUserStats));
    }
}
