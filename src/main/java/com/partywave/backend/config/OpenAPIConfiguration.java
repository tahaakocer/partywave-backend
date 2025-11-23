package com.partywave.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * OpenAPI configuration for PartyWave API documentation.
 * Loads OpenAPI specification from openapi.yml file and merges paths with controller-detected endpoints.
 */
@Configuration
public class OpenAPIConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAPIConfiguration.class);

    @Bean
    public OpenAPI partyWaveOpenAPI() {
        try {
            // Load YAML file from classpath
            ClassPathResource resource = new ClassPathResource("openapi.yml");
            if (!resource.exists()) {
                LOG.warn("openapi.yml file not found, using default configuration");
                return createDefaultOpenAPI();
            }

            try (InputStream inputStream = resource.getInputStream()) {
                // Parse YAML file using Jackson YAML
                ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                OpenAPI openAPI = yamlMapper.readValue(inputStream, OpenAPI.class);

                if (openAPI != null) {
                    LOG.info("Successfully loaded OpenAPI specification from openapi.yml");
                    return openAPI;
                } else {
                    LOG.error("Failed to parse openapi.yml");
                    return createDefaultOpenAPI();
                }
            }
        } catch (Exception e) {
            LOG.error("Error loading openapi.yml file, using default configuration", e);
            return createDefaultOpenAPI();
        }
    }

    /**
     * Customizer to merge YAML paths with controller-detected paths.
     * This ensures that YAML documentation (descriptions, examples, etc.) is preserved
     * while controller-detected endpoints are still included.
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            try {
                // Load YAML file again to get paths
                ClassPathResource resource = new ClassPathResource("openapi.yml");
                if (resource.exists()) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                        OpenAPI yamlOpenAPI = yamlMapper.readValue(inputStream, OpenAPI.class);

                        if (yamlOpenAPI != null && yamlOpenAPI.getPaths() != null) {
                            // Merge YAML paths with existing paths
                            // YAML paths will override/merge with controller-detected paths
                            Map<String, PathItem> yamlPaths = yamlOpenAPI.getPaths();
                            if (openApi.getPaths() == null) {
                                openApi.setPaths(yamlOpenAPI.getPaths());
                            } else {
                                // Merge paths: YAML documentation takes precedence for descriptions, etc.
                                yamlPaths.forEach((path, pathItem) -> {
                                    if (openApi.getPaths().containsKey(path)) {
                                        // Merge path items - YAML documentation enriches controller-detected paths
                                        PathItem existingPathItem = openApi.getPaths().get(path);
                                        mergePathItem(existingPathItem, pathItem);
                                    } else {
                                        // Add new path from YAML
                                        openApi.getPaths().addPathItem(path, pathItem);
                                    }
                                });
                            }
                            LOG.debug("Merged {} paths from openapi.yml", yamlPaths.size());
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error merging YAML paths, using controller-detected paths only", e);
            }
        };
    }

    /**
     * Merge YAML PathItem documentation into existing PathItem.
     * This preserves controller-detected operations while adding YAML documentation.
     */
    private void mergePathItem(PathItem existing, PathItem yaml) {
        // Merge operations - YAML descriptions and summaries enrich existing operations
        if (yaml.getGet() != null && existing.getGet() != null) {
            mergeOperation(existing.getGet(), yaml.getGet());
        }
        if (yaml.getPost() != null && existing.getPost() != null) {
            mergeOperation(existing.getPost(), yaml.getPost());
        }
        if (yaml.getPut() != null && existing.getPut() != null) {
            mergeOperation(existing.getPut(), yaml.getPut());
        }
        if (yaml.getDelete() != null && existing.getDelete() != null) {
            mergeOperation(existing.getDelete(), yaml.getDelete());
        }
        if (yaml.getPatch() != null && existing.getPatch() != null) {
            mergeOperation(existing.getPatch(), yaml.getPatch());
        }
    }

    /**
     * Merge operation documentation from YAML into existing operation.
     */
    private void mergeOperation(io.swagger.v3.oas.models.Operation existing, io.swagger.v3.oas.models.Operation yaml) {
        // Preserve YAML descriptions and summaries
        if (yaml.getDescription() != null && !yaml.getDescription().isEmpty()) {
            existing.setDescription(yaml.getDescription());
        }
        if (yaml.getSummary() != null && !yaml.getSummary().isEmpty()) {
            existing.setSummary(yaml.getSummary());
        }
        // Merge tags
        if (yaml.getTags() != null && !yaml.getTags().isEmpty()) {
            existing.setTags(yaml.getTags());
        }
        // Merge request body documentation
        if (yaml.getRequestBody() != null) {
            existing.setRequestBody(yaml.getRequestBody());
        }
        // Merge responses - YAML responses enrich existing ones
        if (yaml.getResponses() != null) {
            if (existing.getResponses() == null) {
                existing.setResponses(yaml.getResponses());
            } else {
                yaml
                    .getResponses()
                    .forEach((code, response) -> {
                        if (existing.getResponses().containsKey(code)) {
                            // Merge response - YAML description takes precedence
                            io.swagger.v3.oas.models.responses.ApiResponse existingResponse = existing.getResponses().get(code);
                            if (response.getDescription() != null) {
                                existingResponse.setDescription(response.getDescription());
                            }
                            if (response.getContent() != null) {
                                existingResponse.setContent(response.getContent());
                            }
                        } else {
                            existing.getResponses().addApiResponse(code, response);
                        }
                    });
            }
        }
    }

    private OpenAPI createDefaultOpenAPI() {
        return new OpenAPI()
            .info(
                new io.swagger.v3.oas.models.info.Info().title("PartyWave API").description("PartyWave API documentation").version("0.0.1")
            );
    }
}
