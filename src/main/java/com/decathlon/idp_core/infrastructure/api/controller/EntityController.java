package com.decathlon.idp_core.infrastructure.api.controller;

import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.BAD_REQUEST_CODE;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.CREATED_CODE;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITIES_PAGINATED_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITIES_SUMMARY;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_BY_IDENTIFIER_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.ENDPOINT_GET_ENTITY_BY_IDENTIFIER_SUMMARY;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.ENDPOINT_POST_ENTITY_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.ENDPOINT_POST_ENTITY_SUMMARY;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.NOT_FOUND_CODE;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.OK_CODE;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.PARAM_PAGE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.PARAM_SIZE_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.PARAM_SORT_DESCRIPTION;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.RESPONSE_ENTITIES_PAGINATED_SUCCESS;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.RESPONSE_ENTITY_CREATED;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.RESPONSE_ENTITY_FOUND;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.RESPONSE_INVALID_ENTITY_DATA;
import static com.decathlon.idp_core.infrastructure.api.configuration.SwaggerDescription.RESPONSE_INVALID_PAGINATION;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.service.EntityService;
import com.decathlon.idp_core.infrastructure.api.configuration.SwaggerConfiguration.EntityPageResponse;
import com.decathlon.idp_core.infrastructure.api.dto.in.EntityDtoIn;
import com.decathlon.idp_core.infrastructure.api.dto.out.entity.EntityDtoOut;
import com.decathlon.idp_core.infrastructure.api.handler.ApiExceptionHandler;
import com.decathlon.idp_core.infrastructure.api.handler.ApiExceptionHandler.ErrorResponse;
import com.decathlon.idp_core.infrastructure.api.mapper.entity.EntityDtoInMapper;
import com.decathlon.idp_core.infrastructure.api.mapper.entity.EntityDtoOutMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

/**
 * REST controller exposing entity management endpoints.
 * <p>
 * Supports:
 * <ul>
 * <li>Paginated retrieval of entities by template identifier</li>
 * <li>Lookup of a single entity by template identifier and entity
 * identifier</li>
 * <li>Creation of new entities for a given template</li>
 * </ul>
 * All endpoints are documented with OpenAPI annotations.
 */
@RestController
@RequestMapping("/api/v1/entities")
@Tag(name = "Entities Management", description = "Operations related to entity management")
@AllArgsConstructor
public class EntityController {

    private final EntityService entityService;
    private final EntityDtoOutMapper entityDtoOutMapper;
    private final EntityDtoInMapper entityDtoInMapper;

    /**
     * Returns a paginated list of entities filtered by template identifier.
     *
     * @param page               zero-based page index (default 0)
     * @param size               page size (default 20)
     * @param templateIdentifier identifier of the template to filter entities
     * @return a page of {@link EntityDtoOut} matching the given template
     */
    @Operation(summary = ENDPOINT_GET_ENTITIES_SUMMARY, description = ENDPOINT_GET_ENTITIES_PAGINATED_DESCRIPTION)
    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITIES_PAGINATED_SUCCESS, content = @Content(schema = @Schema(implementation = EntityPageResponse.class)))
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = {
            @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
    @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
    @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
    @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", defaultValue = "identifier,asc")))
    @ResponseStatus(OK)
    @GetMapping("/{templateIdentifier}")
    public Page<EntityDtoOut> getEntities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @PathVariable String templateIdentifier) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Entity> entities = entityService.getEntitiesByTemplateIdentifier(pageable, templateIdentifier);
        return entityDtoOutMapper.fromEntitiesPageToDtoPage(entities, templateIdentifier);
    }

    /**
     * Retrieves a single entity by template identifier and entity identifier.
     *
     * @param templateIdentifier identifier of the template the entity belongs to
     * @param entityIdentifier   business identifier of the entity
     * @return the matching {@link EntityDtoOut}
     */
    @Operation(summary = ENDPOINT_GET_ENTITY_BY_IDENTIFIER_SUMMARY, description = ENDPOINT_GET_ENTITY_BY_IDENTIFIER_DESCRIPTION)
    @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_FOUND, content = {
            @Content(schema = @Schema(implementation = EntityDtoOut.class)) })
    @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_NOT_FOUND_IDENTIFIER, content = {
            @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class)) })
    @GetMapping("/{templateIdentifier}/identifier/{entityIdentifier}")
    @ResponseStatus(OK)
    public EntityDtoOut getEntity(
            @PathVariable String templateIdentifier,
            @PathVariable String entityIdentifier) {
        Entity entity = entityService.getEntityByTemplateIdentifierAnIdentifier(templateIdentifier, entityIdentifier);
        return entityDtoOutMapper.fromEntity(entity);
    }

    /**
     * Creates a new entity for the given template.
     *
     * @param templateIdentifier identifier of the template the new entity will use
     * @param entityDtoIn        payload with the entity data
     * @return the created {@link EntityDtoOut}
     */
    @Operation(summary = ENDPOINT_POST_ENTITY_SUMMARY, description = ENDPOINT_POST_ENTITY_DESCRIPTION)
    @ApiResponse(responseCode = CREATED_CODE, description = RESPONSE_ENTITY_CREATED, content = {
            @Content(schema = @Schema(implementation = EntityDtoOut.class)) })
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_ENTITY_DATA, content = {
            @Content(schema = @Schema(implementation = ErrorResponse.class)) })
    @PostMapping("/{templateIdentifier}")
    @ResponseStatus(CREATED)
    public EntityDtoOut createEntity(@PathVariable String templateIdentifier, @RequestBody EntityDtoIn entityDtoIn) {
        Entity entity = entityDtoInMapper.fromEntityDtoInToEntity(entityDtoIn, templateIdentifier);
        Entity savedEntity = entityService.createEntity(entity);
        return entityDtoOutMapper.fromEntity(savedEntity);
    }
}
