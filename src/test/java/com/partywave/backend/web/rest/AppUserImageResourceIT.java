package com.partywave.backend.web.rest;

import static com.partywave.backend.domain.AppUserImageAsserts.*;
import static com.partywave.backend.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partywave.backend.IntegrationTest;
import com.partywave.backend.domain.AppUserImage;
import com.partywave.backend.repository.AppUserImageRepository;
import com.partywave.backend.service.AppUserImageService;
import com.partywave.backend.service.dto.AppUserImageDTO;
import com.partywave.backend.service.mapper.AppUserImageMapper;
import jakarta.persistence.EntityManager;
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
 * Integration tests for the {@link AppUserImageResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@WithMockUser
class AppUserImageResourceIT {

    private static final String DEFAULT_URL = "AAAAAAAAAA";
    private static final String UPDATED_URL = "BBBBBBBBBB";

    private static final String DEFAULT_HEIGHT = "AAAAAAAAAA";
    private static final String UPDATED_HEIGHT = "BBBBBBBBBB";

    private static final String DEFAULT_WIDTH = "AAAAAAAAAA";
    private static final String UPDATED_WIDTH = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/app-user-images";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AppUserImageRepository appUserImageRepository;

    @Mock
    private AppUserImageRepository appUserImageRepositoryMock;

    @Autowired
    private AppUserImageMapper appUserImageMapper;

    @Mock
    private AppUserImageService appUserImageServiceMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAppUserImageMockMvc;

    private AppUserImage appUserImage;

    private AppUserImage insertedAppUserImage;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AppUserImage createEntity() {
        return new AppUserImage().url(DEFAULT_URL).height(DEFAULT_HEIGHT).width(DEFAULT_WIDTH);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static AppUserImage createUpdatedEntity() {
        return new AppUserImage().url(UPDATED_URL).height(UPDATED_HEIGHT).width(UPDATED_WIDTH);
    }

    @BeforeEach
    void initTest() {
        appUserImage = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedAppUserImage != null) {
            appUserImageRepository.delete(insertedAppUserImage);
            insertedAppUserImage = null;
        }
    }

    @Test
    @Transactional
    void createAppUserImage() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the AppUserImage
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);
        var returnedAppUserImageDTO = om.readValue(
            restAppUserImageMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(appUserImageDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AppUserImageDTO.class
        );

        // Validate the AppUserImage in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAppUserImage = appUserImageMapper.toEntity(returnedAppUserImageDTO);
        assertAppUserImageUpdatableFieldsEquals(returnedAppUserImage, getPersistedAppUserImage(returnedAppUserImage));

        insertedAppUserImage = returnedAppUserImage;
    }

    @Test
    @Transactional
    void createAppUserImageWithExistingId() throws Exception {
        // Create the AppUserImage with an existing ID
        appUserImage.setId(1L);
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAppUserImageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(appUserImageDTO)))
            .andExpect(status().isBadRequest());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkUrlIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        appUserImage.setUrl(null);

        // Create the AppUserImage, which fails.
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        restAppUserImageMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(appUserImageDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAppUserImages() throws Exception {
        // Initialize the database
        insertedAppUserImage = appUserImageRepository.saveAndFlush(appUserImage);

        // Get all the appUserImageList
        restAppUserImageMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(appUserImage.getId().intValue())))
            .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL)))
            .andExpect(jsonPath("$.[*].height").value(hasItem(DEFAULT_HEIGHT)))
            .andExpect(jsonPath("$.[*].width").value(hasItem(DEFAULT_WIDTH)));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAppUserImagesWithEagerRelationshipsIsEnabled() throws Exception {
        when(appUserImageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAppUserImageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=true")).andExpect(status().isOk());

        verify(appUserImageServiceMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllAppUserImagesWithEagerRelationshipsIsNotEnabled() throws Exception {
        when(appUserImageServiceMock.findAllWithEagerRelationships(any())).thenReturn(new PageImpl(new ArrayList<>()));

        restAppUserImageMockMvc.perform(get(ENTITY_API_URL + "?eagerload=false")).andExpect(status().isOk());
        verify(appUserImageRepositoryMock, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @Transactional
    void getAppUserImage() throws Exception {
        // Initialize the database
        insertedAppUserImage = appUserImageRepository.saveAndFlush(appUserImage);

        // Get the appUserImage
        restAppUserImageMockMvc
            .perform(get(ENTITY_API_URL_ID, appUserImage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(appUserImage.getId().intValue()))
            .andExpect(jsonPath("$.url").value(DEFAULT_URL))
            .andExpect(jsonPath("$.height").value(DEFAULT_HEIGHT))
            .andExpect(jsonPath("$.width").value(DEFAULT_WIDTH));
    }

    @Test
    @Transactional
    void getNonExistingAppUserImage() throws Exception {
        // Get the appUserImage
        restAppUserImageMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAppUserImage() throws Exception {
        // Initialize the database
        insertedAppUserImage = appUserImageRepository.saveAndFlush(appUserImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the appUserImage
        AppUserImage updatedAppUserImage = appUserImageRepository.findById(appUserImage.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAppUserImage are not directly saved in db
        em.detach(updatedAppUserImage);
        updatedAppUserImage.url(UPDATED_URL).height(UPDATED_HEIGHT).width(UPDATED_WIDTH);
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(updatedAppUserImage);

        restAppUserImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, appUserImageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(appUserImageDTO))
            )
            .andExpect(status().isOk());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAppUserImageToMatchAllProperties(updatedAppUserImage);
    }

    @Test
    @Transactional
    void putNonExistingAppUserImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserImage.setId(longCount.incrementAndGet());

        // Create the AppUserImage
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAppUserImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, appUserImageDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(appUserImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAppUserImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserImage.setId(longCount.incrementAndGet());

        // Create the AppUserImage
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserImageMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(appUserImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAppUserImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserImage.setId(longCount.incrementAndGet());

        // Create the AppUserImage
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserImageMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(appUserImageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAppUserImageWithPatch() throws Exception {
        // Initialize the database
        insertedAppUserImage = appUserImageRepository.saveAndFlush(appUserImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the appUserImage using partial update
        AppUserImage partialUpdatedAppUserImage = new AppUserImage();
        partialUpdatedAppUserImage.setId(appUserImage.getId());

        restAppUserImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAppUserImage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAppUserImage))
            )
            .andExpect(status().isOk());

        // Validate the AppUserImage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAppUserImageUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAppUserImage, appUserImage),
            getPersistedAppUserImage(appUserImage)
        );
    }

    @Test
    @Transactional
    void fullUpdateAppUserImageWithPatch() throws Exception {
        // Initialize the database
        insertedAppUserImage = appUserImageRepository.saveAndFlush(appUserImage);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the appUserImage using partial update
        AppUserImage partialUpdatedAppUserImage = new AppUserImage();
        partialUpdatedAppUserImage.setId(appUserImage.getId());

        partialUpdatedAppUserImage.url(UPDATED_URL).height(UPDATED_HEIGHT).width(UPDATED_WIDTH);

        restAppUserImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAppUserImage.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAppUserImage))
            )
            .andExpect(status().isOk());

        // Validate the AppUserImage in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAppUserImageUpdatableFieldsEquals(partialUpdatedAppUserImage, getPersistedAppUserImage(partialUpdatedAppUserImage));
    }

    @Test
    @Transactional
    void patchNonExistingAppUserImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserImage.setId(longCount.incrementAndGet());

        // Create the AppUserImage
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAppUserImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, appUserImageDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(appUserImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAppUserImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserImage.setId(longCount.incrementAndGet());

        // Create the AppUserImage
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserImageMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(appUserImageDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAppUserImage() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        appUserImage.setId(longCount.incrementAndGet());

        // Create the AppUserImage
        AppUserImageDTO appUserImageDTO = appUserImageMapper.toDto(appUserImage);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAppUserImageMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(appUserImageDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the AppUserImage in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAppUserImage() throws Exception {
        // Initialize the database
        insertedAppUserImage = appUserImageRepository.saveAndFlush(appUserImage);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the appUserImage
        restAppUserImageMockMvc
            .perform(delete(ENTITY_API_URL_ID, appUserImage.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return appUserImageRepository.count();
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

    protected AppUserImage getPersistedAppUserImage(AppUserImage appUserImage) {
        return appUserImageRepository.findById(appUserImage.getId()).orElseThrow();
    }

    protected void assertPersistedAppUserImageToMatchAllProperties(AppUserImage expectedAppUserImage) {
        assertAppUserImageAllPropertiesEquals(expectedAppUserImage, getPersistedAppUserImage(expectedAppUserImage));
    }

    protected void assertPersistedAppUserImageToMatchUpdatableProperties(AppUserImage expectedAppUserImage) {
        assertAppUserImageAllUpdatablePropertiesEquals(expectedAppUserImage, getPersistedAppUserImage(expectedAppUserImage));
    }
}
