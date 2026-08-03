package com.decathlon.idp_core.infrastructure.adapters.api.controller;

import static com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerDescription.*;
import static org.springframework.http.HttpStatus.*;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;
import com.decathlon.idp_core.domain.service.entity_dynamic_mapping.EntityDynamicMappingService;
import com.decathlon.idp_core.infrastructure.adapters.api.configuration.SwaggerConfiguration;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingCreateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.in.EntityDynamicMappingUpdateDtoIn;
import com.decathlon.idp_core.infrastructure.adapters.api.dto.out.entity_dynamic_mapping.EntityDynamicMappingDtoOut;
import com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler;
import com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity_dynamic_mapping.EntityDynamicMappingMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/entity_dynamic_mappings")
@Tag(name = "Entity dynamic mapping", description = "Operations related to entity dynamic mapping management")
public class EntityDynamicMappingController {

  private final EntityDynamicMappingMapper dynamicMappingMapper;
  private final EntityDynamicMappingService dynamicMappingService;

  @Operation(summary = ENDPOINT_POST_ENTITY_DYNAMIC_MAPPING_SUMMARY, description = ENDPOINT_POST_ENTITY_DYNAMIC_MAPPING_DESCRIPTION)
  @ApiResponse(responseCode = CREATED_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_CREATED)
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_DATA)
  @ApiResponse(responseCode = CONFLICT_CODE, description = "Identifier already exists")
  @PostMapping
  @ResponseStatus(CREATED)
  public EntityDynamicMappingDtoOut createDynamicMapping(
      @Valid @RequestBody EntityDynamicMappingCreateDtoIn inboundWebhookMappingDtoIn) {
    EntityDynamicMapping entityDynamicMapping = dynamicMappingService
        .createEntityDynamicMapping(dynamicMappingMapper.toDomain(inboundWebhookMappingDtoIn));
    return dynamicMappingMapper.fromEntityMappingToDto(entityDynamicMapping);
  }

  @Operation(summary = ENDPOINT_GET_ENTITY_DYNAMIC_MAPPING_PAGINATED_SUMMARY, description = ENDPOINT_GET_ENTITY_DYNAMIC_MAPPING_PAGINATED_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_PAGINATED_SUCCESS, content = @Content(schema = @Schema(implementation = SwaggerConfiguration.EntityDynamicMappingPageResponse.class)))
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = RESPONSE_INVALID_PAGINATION, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @Parameter(name = "page", description = PARAM_PAGE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "0")))
  @Parameter(name = "size", description = PARAM_SIZE_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "integer", defaultValue = "20")))
  @Parameter(name = "sort", description = PARAM_SORT_DESCRIPTION, in = ParameterIn.QUERY, content = @Content(schema = @Schema(type = "string", defaultValue = "identifier,asc")))
  @GetMapping
  @ResponseStatus(OK)
  public Page<EntityDynamicMappingDtoOut> getEntityDynamicMappingPaginated(
      @PageableDefault(size = 20, sort = "identifier") @Parameter(hidden = true) Pageable pageable) {
    return dynamicMappingService.getAllEntityDynamicMapping(pageable)
        .map(dynamicMappingMapper::fromEntityMappingToDto);
  }

  @Operation(summary = ENDPOINT_DELETE_ENTITY_DYNAMIC_MAPPING_SUMMARY, description = ENDPOINT_DELETE_ENTITY_DYNAMIC_MAPPING_DESCRIPTION)
  @ApiResponse(responseCode = NO_CONTENT_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_DELETED)
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @ResponseStatus(NO_CONTENT)
  @DeleteMapping("/{identifier}")
  public void deleteEntityDynamicMapping(@PathVariable String identifier) {
    dynamicMappingService.deleteEntityDynamicMapping(identifier);
  }

  @Operation(summary = ENDPOINT_GET_ENTITY_DYNAMIC_MAPPING_BY_IDENTIFIER_SUMMARY, description = ENDPOINT_GET_ENTITY_DYNAMIC_MAPPING_BY_IDENTIFIER_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_FOUND, content = {
      @Content(schema = @Schema(implementation = EntityDynamicMappingDtoOut.class))})
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @GetMapping("/{identifier}")
  @ResponseStatus(OK)
  public EntityDynamicMappingDtoOut getEntityDynamicMappingByIdentifier(
      @PathVariable String identifier) {
    EntityDynamicMapping entityDynamicMapping = dynamicMappingService
        .getEntityDynamicMapping(identifier);
    return dynamicMappingMapper.fromEntityMappingToDto(entityDynamicMapping);
  }

  @Operation(summary = ENDPOINT_PUT_ENTITY_DYNAMIC_MAPPING_SUMMARY, description = ENDPOINT_PUT_ENTITY_DYNAMIC_MAPPING_DESCRIPTION)
  @ApiResponse(responseCode = OK_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_UPDATED, content = {
      @Content(schema = @Schema(implementation = EntityDynamicMappingDtoOut.class))})
  @ApiResponse(responseCode = NOT_FOUND_CODE, description = RESPONSE_ENTITY_DYNAMIC_MAPPING_NOT_FOUND_IDENTIFIER, content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "", content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @ApiResponse(responseCode = CONFLICT_CODE, description = "", content = {
      @Content(schema = @Schema(implementation = ApiExceptionHandler.ErrorResponse.class))})
  @PutMapping("/{identifier}")
  @ResponseStatus(OK)
  public EntityDynamicMappingDtoOut updateEntityDynamicMapping(@PathVariable String identifier,
      @Valid @RequestBody EntityDynamicMappingUpdateDtoIn entityDynamicMappingDtoIn) {
    return dynamicMappingMapper
        .fromEntityMappingToDto(dynamicMappingService.updateEntityDynamicMapping(identifier,
            dynamicMappingMapper.toDomainForUpdate(identifier, entityDynamicMappingDtoIn)));
  }
}
