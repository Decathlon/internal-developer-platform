package com.decathlon.idp_core.domain.service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.decathlon.idp_core.domain.constant.SearchConstraints;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityCompositeKey;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity.FilterCriterion;
import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.model.enums.FilterKeyType;
import com.decathlon.idp_core.domain.model.enums.FilterOperator;
import com.decathlon.idp_core.domain.model.search.LogicalConnector;
import com.decathlon.idp_core.domain.model.search.PaginatedResult;
import com.decathlon.idp_core.domain.model.search.PaginationCriteria;
import com.decathlon.idp_core.domain.model.search.SearchFilterNode;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;
import com.decathlon.idp_core.domain.service.filter.EntityFilterDslParser;
import com.decathlon.idp_core.domain.service.search.SearchFilterValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityService Tests")
class EntityServiceTest {

  @Mock
  private EntityRepositoryPort entityRepository;

  @Mock
  private EntityValidationService entityValidationService;

  @Mock
  private EntityTemplateValidationService entityTemplateValidationService;

  @Mock
  private EntityTemplateService entityTemplateService;

  @Mock
  private EntityFilterDslParser entityFilterDslParser;

  @Mock
  private SearchFilterValidationService searchFilterValidationService;

  @InjectMocks
  private EntityService entityService;

  @Test
  @DisplayName("Should return entities page by template identifier")
  void shouldReturnEntitiesByTemplateIdentifier() {
    var pageable = Pageable.ofSize(10);
    var entity = entity("template-a", "entity-a", "Entity A");
    var page = new PageImpl<>(List.of(entity));
    var template = new EntityTemplate(UUID.randomUUID(), "template-a", "Template A", "desc",
        List.of(), List.of());

    when(entityTemplateService.getEntityTemplateByIdentifier("template-a")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierWithFilter("template-a", EntityFilter.empty(),
        pageable)).thenReturn(page);

    var result = entityService.getEntitiesByTemplateIdentifier(pageable, "template-a", null);

    assertSame(page, result);
    verify(entityTemplateService).getEntityTemplateByIdentifier("template-a");
    verify(entityFilterDslParser).validateFilterPropertyTypes(EntityFilter.empty(), template);
    verify(entityRepository).findByTemplateIdentifierWithFilter("template-a", EntityFilter.empty(),
        pageable);
  }

  @Test
  @DisplayName("Should return entity summaries by composite keys")
  void shouldReturnEntitySummariesByCompositeKeys() {
    var compositeKeys = List.of(new EntityCompositeKey("web-service", "service-a"));
    var summaries = List.of(new EntitySummary("service-a", "Service A", "web-service"));
    when(entityRepository.findSummariesByCompositeKeys(compositeKeys)).thenReturn(summaries);

    var result = entityService.getEntitiesSummariesByCompositeKeys(compositeKeys);

    assertEquals(summaries, result);
    verify(entityRepository).findSummariesByCompositeKeys(compositeKeys);
  }

  @Test
  @DisplayName("Should return entity by template and identifier")
  void shouldReturnEntityByTemplateAndIdentifier() {
    var entity = entity("web-service", "catalog-api", "Catalog API");
    when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
        .thenReturn(Optional.of(entity));

    var result = entityService.getEntityByTemplateIdentifierAndIdentifier("web-service",
        "catalog-api");

    assertSame(entity, result);
    verify(entityTemplateValidationService).validateTemplateExists("web-service");
    verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "catalog-api");
  }

  @Test
  @DisplayName("Should throw when entity is not found for template")
  void shouldThrowWhenEntityNotFoundByTemplateAndIdentifier() {
    when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "missing-entity"))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> entityService
        .getEntityByTemplateIdentifierAndIdentifier("web-service", "missing-entity"));
  }

  @Test
  @DisplayName("Should create entity when validations pass")
  void shouldCreateEntityWhenValidationsPass() {
    var entity = entity("web-service", "catalog-api", "Catalog API");
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        List.of(), List.of());
    when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
    when(entityRepository.save(entity)).thenReturn(entity);

    var result = entityService.createEntity(entity);

    assertSame(entity, result);

    InOrder inOrder = inOrder(entityTemplateService, entityValidationService, entityRepository);
    inOrder.verify(entityTemplateService).getEntityTemplateByIdentifier("web-service");
    inOrder.verify(entityValidationService).validateForCreation(entity, template);
    inOrder.verify(entityRepository).save(entity);
    verifyNoInteractions(entityTemplateValidationService);
  }

  @Test
  @DisplayName("Should not save when entity already exists")
  void shouldNotSaveWhenEntityAlreadyExists() {
    var entity = entity("web-service", "catalog-api", "Catalog API");
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        List.of(), List.of());
    var alreadyExists = new EntityAlreadyExistsException("web-service", "catalog-api");

    when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
    doThrow(alreadyExists).when(entityValidationService).validateForCreation(entity, template);

    assertThrows(EntityAlreadyExistsException.class, () -> entityService.createEntity(entity));

    verify(entityTemplateService).getEntityTemplateByIdentifier("web-service");
    verify(entityValidationService).validateForCreation(entity, template);
    verifyNoMoreInteractions(entityRepository);
  }

  @Test
  @DisplayName("Should stop immediately when template does not exist")
  void shouldStopWhenTemplateDoesNotExistOnCreate() {
    var entity = entity("missing-template", "catalog-api", "Catalog API");

    when(entityTemplateService.getEntityTemplateByIdentifier("missing-template"))
        .thenThrow(new EntityTemplateNotFoundException("identifier", "missing-template"));

    assertThrows(EntityTemplateNotFoundException.class, () -> entityService.createEntity(entity));

    verify(entityTemplateService).getEntityTemplateByIdentifier("missing-template");
    verifyNoInteractions(entityValidationService);
    verifyNoMoreInteractions(entityRepository);
  }

  @Test
  @DisplayName("Should update entity when validations pass")
  void shouldUpdateEntityWhenValidationsPass() {
    var existing = new Entity(UUID.randomUUID(), "web-service", "Web API 2", "web-api-2", List.of(),
        List.of());
    var payload = new Entity(null, "web-service", "Web API 2 Updated", "web-api-2", List.of(),
        List.of());
    var expectedSaved = new Entity(existing.id(), "web-service", "Web API 2 Updated", "web-api-2",
        List.of(), List.of());
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        List.of(), List.of());

    when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "web-api-2"))
        .thenReturn(Optional.of(existing));
    when(entityRepository.save(expectedSaved)).thenReturn(expectedSaved);

    var result = entityService.updateEntity("web-service", "web-api-2", payload);

    assertSame(expectedSaved, result);
    InOrder inOrder = inOrder(entityTemplateService, entityRepository, entityValidationService,
        entityRepository);
    inOrder.verify(entityTemplateService).getEntityTemplateByIdentifier("web-service");
    inOrder.verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service",
        "web-api-2");
    inOrder.verify(entityValidationService).validateForUpdate(expectedSaved, template);
    inOrder.verify(entityRepository).save(expectedSaved);
  }

  @Test
  @DisplayName("Should throw when updating non-existing entity")
  void shouldThrowWhenUpdatingNonExistingEntity() {
    var payload = new Entity(null, "web-service", "Web API 2 Updated", "web-api-2", List.of(),
        List.of());
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        List.of(), List.of());

    when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "web-api-2"))
        .thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> entityService.updateEntity("web-service", "web-api-2", payload));

    verify(entityTemplateService).getEntityTemplateByIdentifier("web-service");
    verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "web-api-2");
    verifyNoMoreInteractions(entityRepository);
  }

  @Test
  @DisplayName("Should propagate two validation errors when update payload violates template constraints")
  void shouldPropagateTwoValidationErrorsWhenUpdatingInvalidEntity() {
    var existing = new Entity(UUID.randomUUID(), "web-service", "Web API 2", "web-api-2", List.of(),
        List.of());
    var payload = new Entity(null, "web-service", "Web API 2 Updated", "web-api-2", List.of(),
        List.of());
    var expectedToValidate = new Entity(existing.id(), "web-service", "Web API 2 Updated",
        "web-api-2", List.of(), List.of());
    var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc",
        List.of(), List.of());
    var validationErrors = List.of(
        ValidationMessages.PROPERTY_NOT_DEFINED_IN_TEMPLATE.formatted("status", "web-service"),
        ValidationMessages.RELATION_TARGET_ENTITY_NOT_FOUND.formatted("child_of",
            "missing-platform"));

    when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "web-api-2"))
        .thenReturn(Optional.of(existing));
    doThrow(new EntityValidationException(validationErrors)).when(entityValidationService)
        .validateForUpdate(expectedToValidate, template);

    var thrown = assertThrows(EntityValidationException.class,
        () -> entityService.updateEntity("web-service", "web-api-2", payload));

    assertEquals(validationErrors, thrown.getViolations());
    verify(entityValidationService).validateForUpdate(expectedToValidate, template);
    verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "web-api-2");
    verifyNoMoreInteractions(entityRepository);
  }

  @Test
  @DisplayName("Should successfully delete entity and remove its reference from parent entity relations")
  void shouldDeleteEntityAndRemoveReferenceFromParent() {
    // 1. Arrange
    String targetTemplateId = "service";
    String targetEntityId = "payment-api";
    var targetEntity = new Entity(UUID.randomUUID(), targetTemplateId, "Payment API",
        targetEntityId, List.of(), List.of());

    String parentTemplateId = "application";
    String parentEntityId = "e-commerce-app";
    UUID relationId = UUID.randomUUID();

    // Parent currently has a relation targeting BOTH "payment-api" and "auth-api"
    var parentEntity = new Entity(UUID.randomUUID(), parentTemplateId, "E-Commerce App",
        parentEntityId, List.of(), List.of(new Relation(relationId, "dependencies",
            targetTemplateId, List.of(targetEntityId, "auth-api"))));

    // Parent template allows multiple dependencies (not a blocking required 1-to-1
    // relation)
    var parentTemplate = new EntityTemplate(UUID.randomUUID(), parentTemplateId, "Application",
        "desc", List.of(), List.of(new RelationDefinition(UUID.randomUUID(), "dependencies",
            targetTemplateId, false, true)));

    // Expected parent after cleanup: the relation only contains "auth-api"
    var expectedParentAfterCleanup = new Entity(parentEntity.id(), parentTemplateId,
        parentEntity.name(), parentEntityId, List.of(),
        List.of(new Relation(relationId, "dependencies", targetTemplateId, List.of("auth-api"))));

    // Setup Mocks
    when(entityRepository.findByTemplateIdentifierAndIdentifier(targetTemplateId, targetEntityId))
        .thenReturn(Optional.of(targetEntity));
    when(entityRepository.findEntitiesRelated(targetEntityId)).thenReturn(List.of(parentEntity));
    when(entityTemplateService.getEntityTemplateByIdentifier(parentTemplateId))
        .thenReturn(parentTemplate);

    // 2. Act
    entityService.deleteEntity(targetTemplateId, targetEntityId);

    // 3. Assert using InOrder to guarantee the exact sequence of business
    // operations
    InOrder inOrder = inOrder(entityTemplateValidationService, entityRepository,
        entityTemplateService);

    // Validates target template exists
    inOrder.verify(entityTemplateValidationService).validateTemplateExists(targetTemplateId);

    // Retrieves target entity to delete
    inOrder.verify(entityRepository).findByTemplateIdentifierAndIdentifier(targetTemplateId,
        targetEntityId);

    // Finds parent entities pointing to target entity
    inOrder.verify(entityRepository).findEntitiesRelated(targetEntityId);

    // Retrieves parent template to evaluate relation constraints safely
    inOrder.verify(entityTemplateService).getEntityTemplateByIdentifier(parentTemplateId);

    // Saves the parent with the target ID cleanly removed from its relations
    inOrder.verify(entityRepository).save(expectedParentAfterCleanup);

    // Finally deletes the target entity
    inOrder.verify(entityRepository).deleteByTemplateIdentifierAndIdentifier(targetTemplateId,
        targetEntityId);
  }

  @Test
  @DisplayName("Should throw EntityDeletionBlockedException when target entity is referenced by a required relation")
  void shouldThrowExceptionWhenTargetEntityReferencedByRequiredRelation() {
    var relationId = UUID.randomUUID();
    var parent = new Entity(UUID.randomUUID(), "application", "Application A", "app-a", List.of(),
        List.of(new Relation(relationId, "owner", "team", List.of("team-a"))));

    var parentTemplate = new EntityTemplate(UUID.randomUUID(), "application", "Application", "desc",
        List.of(),
        List.of(new RelationDefinition(UUID.randomUUID(), "owner", "team", true, false)));

    when(entityRepository.findByTemplateIdentifierAndIdentifier("team", "team-a"))
        .thenReturn(Optional
            .of(new Entity(UUID.randomUUID(), "team", "team-a", "team-a", List.of(), List.of())));

    when(entityRepository.findEntitiesRelated("team-a")).thenReturn(List.of(parent));

    // The fixed service will now ask for the PARENT's template
    when(entityTemplateService.getEntityTemplateByIdentifier("application"))
        .thenReturn(parentTemplate);

    assertThrows(EntityDeletionBlockedException.class,
        () -> entityService.deleteEntity("team", "team-a"));

    // Verify deletion is completely blocked
    verify(entityTemplateValidationService).validateTemplateExists("team");
    verify(entityRepository, never()).save(any());
    verify(entityRepository, never()).deleteByTemplateIdentifierAndIdentifier(anyString(),
        anyString());
  }

  @Test
  @DisplayName("Should keep relation and remove only deleted target when relation still has targets")
  void shouldKeepRelationAndRemoveOnlyDeletedTargetWhenRelationStillHasTargets() {
    var relationId = UUID.randomUUID();
    var parent = new Entity(UUID.randomUUID(), "application", "Application A", "app-a", List.of(),
        List.of(
            new Relation(relationId, "dependencies", "service", List.of("catalog", "billing"))));

    var parentTemplate = new EntityTemplate(UUID.randomUUID(), "application", "Application", "desc",
        List.of(),
        List.of(new RelationDefinition(UUID.randomUUID(), "dependencies", "service", false, true)));

    // Fixed typo: "catalog" is a "service", so we mock and delete ("service",
    // "catalog")
    when(entityRepository.findByTemplateIdentifierAndIdentifier("service", "catalog"))
        .thenReturn(Optional.of(
            new Entity(UUID.randomUUID(), "service", "catalog", "catalog", List.of(), List.of())));

    // The entity we are deleting is "catalog"
    when(entityRepository.findEntitiesRelated("catalog")).thenReturn(List.of(parent));
    when(entityTemplateService.getEntityTemplateByIdentifier("application"))
        .thenReturn(parentTemplate);

    // Call service with correct parameters
    entityService.deleteEntity("service", "catalog");

    var expectedParentAfterCleanup = new Entity(parent.id(), parent.templateIdentifier(),
        parent.name(), parent.identifier(), parent.properties(),
        List.of(new Relation(relationId, "dependencies", "service", List.of("billing"))));

    verify(entityTemplateValidationService).validateTemplateExists("service");
    verify(entityRepository).save(expectedParentAfterCleanup);
    verify(entityRepository).deleteByTemplateIdentifierAndIdentifier("service", "catalog");
  }

  @Test
  @DisplayName("Should throw EntityDeletionBlockedException when target entity is the last one in a required toMany relation")
  void shouldThrowExceptionWhenTargetIsLastInRequiredToManyRelation() {
    var relationId = UUID.randomUUID();
    var parent = new Entity(UUID.randomUUID(), "cluster", "Production Cluster", "prod-cluster",
        List.of(), List.of(new Relation(relationId, "nodes", "server", List.of("server-1"))));

    var parentTemplate = new EntityTemplate(UUID.randomUUID(), "cluster", "Cluster", "desc",
        List.of(),
        List.of(new RelationDefinition(UUID.randomUUID(), "nodes", "server", true, true)));

    when(entityRepository.findByTemplateIdentifierAndIdentifier("server", "server-1"))
        .thenReturn(Optional.of(
            new Entity(UUID.randomUUID(), "server", "server-1", "server-1", List.of(), List.of())));
    when(entityRepository.findEntitiesRelated("server-1")).thenReturn(List.of(parent));
    when(entityTemplateService.getEntityTemplateByIdentifier("cluster")).thenReturn(parentTemplate);

    assertThrows(EntityDeletionBlockedException.class,
        () -> entityService.deleteEntity("server", "server-1"));
    verify(entityTemplateValidationService).validateTemplateExists("server");
    verify(entityRepository, never()).save(any());
    verify(entityRepository, never()).deleteByTemplateIdentifierAndIdentifier(anyString(),
        anyString());
  }

  private Entity entity(String templateIdentifier, String identifier, String name) {
    return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(),
        List.of());
  }

  private SearchFilterNode emptyFilter() {
    return new SearchFilterNode.Group(LogicalConnector.AND, List.of());
  }

  private void assertSearchThrows(PaginationCriteria paginationCriteria) {
    var filter = emptyFilter();
    assertThrows(InvalidSearchQueryException.class,
        () -> entityService.searchEntities(filter, null, paginationCriteria));
  }

  private Property property(String name, String value) {
    return new Property(UUID.randomUUID(), name, value);
  }

  private Relation relation(String name, String targetTemplateIdentifier, String... targetIds) {
    return new Relation(UUID.randomUUID(), name, targetTemplateIdentifier, List.of(targetIds));
  }

  private Relation relation(UUID id, String name, String targetTemplateIdentifier,
      String... targetIds) {
    return new Relation(id, name, targetTemplateIdentifier, List.of(targetIds));
  }

  private RelationDefinition relationDefinition(String name, String targetTemplateIdentifier,
      boolean required, boolean toMany) {
    return new RelationDefinition(UUID.randomUUID(), name, targetTemplateIdentifier, required,
        toMany);
  }

  private EntityTemplate templateWithRelations(String identifier,
      RelationDefinition... relationDefinitions) {
    return new EntityTemplate(UUID.randomUUID(), identifier, identifier, "desc", List.of(),
        List.of(relationDefinitions));
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivateMethod(String methodName, Object... args) {
    return (T) ReflectionTestUtils.invokeMethod(entityService, methodName, args);
  }

  private void verifyNoCollaboratorInteractions() {
    verifyNoInteractions(entityRepository, entityValidationService, entityTemplateValidationService,
        entityTemplateService, entityFilterDslParser, searchFilterValidationService);
  }

  @Test
  @DisplayName("Should search entities with valid parameters")
  void shouldSearchEntitiesWithValidParameters() {
    var filter = emptyFilter();
    var entity = entity("tmpl", "ent-a", "Entity A");
    var paginatedResult = new PaginatedResult<>(List.of(entity), 1L, 1, 0);
    when(entityRepository.search(filter, "api", new PaginationCriteria(0, 20, null)))
        .thenReturn(paginatedResult);

    var result = entityService.searchEntities(filter, "api", new PaginationCriteria(0, 20, null));

    assertEquals(paginatedResult, result);
    verify(searchFilterValidationService).validate(filter, "api");
  }

  @Test
  @DisplayName("Should search entities with valid sort")
  void shouldSearchEntitiesWithValidSort() {
    var filter = emptyFilter();
    var entity = entity("tmpl", "ent-a", "Entity A");
    var paginatedResult = new PaginatedResult<>(List.of(entity), 1L, 1, 0);
    when(entityRepository.search(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(paginatedResult);

    var result = entityService.searchEntities(filter, null,
        new PaginationCriteria(0, 10, "identifier:asc"));

    assertEquals(paginatedResult, result);
  }

  @Test
  @DisplayName("Should reject page size exceeding maximum")
  void shouldRejectPageSizeExceedingMaximum() {
    assertSearchThrows(new PaginationCriteria(0, SearchConstraints.MAX_PAGE_SIZE + 1, null));
  }

  @Test
  @DisplayName("Should reject negative page index")
  void shouldRejectNegativePageIndex() {
    assertSearchThrows(new PaginationCriteria(-1, 20, null));
  }

  @Test
  @DisplayName("Should reject non-positive page size")
  void shouldRejectNonPositivePageSize() {
    assertSearchThrows(new PaginationCriteria(0, 0, null));
  }

  @Test
  @DisplayName("Should reject invalid sort field")
  void shouldRejectInvalidSortField() {
    assertSearchThrows(new PaginationCriteria(0, 20, "badField:asc"));
  }

  @Test
  @DisplayName("Should reject invalid sort direction")
  void shouldRejectInvalidSortDirection() {
    assertSearchThrows(new PaginationCriteria(0, 20, "identifier:zzz"));
  }

  @Test
  @DisplayName("Should reject extra sort expression segments")
  void shouldRejectExtraSortSegments() {
    assertSearchThrows(new PaginationCriteria(0, 20, "identifier:asc:extra"));
  }

  // ---------------------------------------------------------------------------
  // Additional coverage for EntityService private business rules
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return entities page when non-null filter is provided")
  void shouldReturnEntitiesByTemplateIdentifierWhenFilterProvided() {
    // Arrange
    var pageable = Pageable.ofSize(5);
    var filter = new EntityFilter(List.of(
        new FilterCriterion(FilterKeyType.ATTRIBUTE, "name", FilterOperator.CONTAINS, "catalog")));
    var template = templateWithRelations("template-a");
    var entity = entity("template-a", "catalog-api", "Catalog API");
    var page = new PageImpl<>(List.of(entity));

    when(entityTemplateService.getEntityTemplateByIdentifier("template-a")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierWithFilter("template-a", filter, pageable))
        .thenReturn(page);

    // Act
    var result = entityService.getEntitiesByTemplateIdentifier(pageable, "template-a", filter);

    // Assert
    assertSame(page, result);
    verify(entityFilterDslParser).validateFilterPropertyTypes(filter, template);
    verify(entityRepository).findByTemplateIdentifierWithFilter("template-a", filter, pageable);
  }

  @Test
  @DisplayName("Should enrich relation target templates when creating entity")
  void shouldEnrichRelationTargetTemplatesWhenCreatingEntity() {
    // Arrange
    var ownerRelation = relation("owner", "ignored-template", "team-a");
    var legacyRelation = relation("legacy-link", "legacy-template", "legacy-a");
    var entity = new Entity(UUID.randomUUID(), "application", "Commerce App", "commerce-app",
        List.of(), List.of(ownerRelation, legacyRelation));
    var template = templateWithRelations("application",
        relationDefinition("owner", "team", true, false));
    var expectedSaved = new Entity(entity.id(), "application", "Commerce App", "commerce-app",
        List.of(), List.of(relation(ownerRelation.id(), "owner", "team", "team-a"),
            relation(legacyRelation.id(), "legacy-link", "legacy-template", "legacy-a")));

    when(entityTemplateService.getEntityTemplateByIdentifier("application")).thenReturn(template);
    when(entityRepository.save(expectedSaved)).thenReturn(expectedSaved);

    // Act
    var result = entityService.createEntity(entity);

    // Assert
    assertEquals(expectedSaved, result);
    verify(entityValidationService).validateForCreation(expectedSaved, template);
    verify(entityRepository).save(expectedSaved);
  }

  @Test
  @DisplayName("Should save updated entity when properties change")
  void shouldSaveUpdatedEntityWhenPropertiesChange() {
    // Arrange
    var existing = new Entity(UUID.randomUUID(), "web-service", "Catalog API", "catalog-api",
        List.of(property("language", "java")), List.of());
    var payload = new Entity(null, "web-service", "Catalog API", "catalog-api",
        List.of(new Property(null, "language", "kotlin")), List.of());
    var expectedSaved = new Entity(existing.id(), "web-service", "Catalog API", "catalog-api",
        payload.properties(), List.of());
    var template = templateWithRelations("web-service");

    when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
        .thenReturn(Optional.of(existing));
    when(entityRepository.save(expectedSaved)).thenReturn(expectedSaved);

    // Act
    var result = entityService.updateEntity("web-service", "catalog-api", payload);

    // Assert
    assertSame(expectedSaved, result);
    verify(entityValidationService).validateForUpdate(expectedSaved, template);
    verify(entityRepository).save(expectedSaved);
  }

  @Test
  @DisplayName("Should save updated entity when relations change")
  void shouldSaveUpdatedEntityWhenRelationsChange() {
    // Arrange
    var existingRelation = relation("owner", "team", "team-a");
    var payloadRelation = relation("owner", "placeholder", "team-b");
    var existing = new Entity(UUID.randomUUID(), "application", "Commerce App", "commerce-app",
        List.of(), List.of(existingRelation));
    var payload = new Entity(null, "application", "Commerce App", "commerce-app", List.of(),
        List.of(payloadRelation));
    var template = templateWithRelations("application",
        relationDefinition("owner", "team", true, false));
    var expectedSaved = new Entity(existing.id(), "application", "Commerce App", "commerce-app",
        List.of(), List.of(relation(payloadRelation.id(), "owner", "team", "team-b")));

    when(entityTemplateService.getEntityTemplateByIdentifier("application")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierAndIdentifier("application", "commerce-app"))
        .thenReturn(Optional.of(existing));
    when(entityRepository.save(expectedSaved)).thenReturn(expectedSaved);

    // Act
    var result = entityService.updateEntity("application", "commerce-app", payload);

    // Assert
    assertSame(expectedSaved, result);
    verify(entityValidationService).validateForUpdate(expectedSaved, template);
    verify(entityRepository).save(expectedSaved);
  }

  @Test
  @DisplayName("Should return existing entity when update does not change content")
  void shouldReturnExistingEntityWhenUpdateDoesNotChangeContent() {
    // Arrange
    var existing = new Entity(UUID.randomUUID(), "application", "Commerce App", "commerce-app",
        List.of(property("language", "java")),
        List.of(relation("owner", "team", "team-a", "team-b")));
    var payload = new Entity(null, "application", "Commerce App", "commerce-app",
        List.of(new Property(null, "language", "java")),
        List.of(new Relation(null, "owner", "placeholder", List.of("team-b", "team-a"))));
    var template = templateWithRelations("application",
        relationDefinition("owner", "team", true, true));
    var expectedValidated = new Entity(existing.id(), "application", "Commerce App", "commerce-app",
        payload.properties(),
        List.of(new Relation(null, "owner", "team", List.of("team-b", "team-a"))));

    when(entityTemplateService.getEntityTemplateByIdentifier("application")).thenReturn(template);
    when(entityRepository.findByTemplateIdentifierAndIdentifier("application", "commerce-app"))
        .thenReturn(Optional.of(existing));

    // Act
    var result = entityService.updateEntity("application", "commerce-app", payload);

    // Assert
    assertSame(existing, result);
    verify(entityValidationService).validateForUpdate(expectedValidated, template);
    verify(entityRepository, never()).save(any());
  }

  @Test
  @DisplayName("Should treat two null property lists as equal")
  void shouldTreatTwoNullPropertyListsAsEqual() {
    // Act
    Boolean result = invokePrivateMethod("havePropertiesChanged", (Object) null, (Object) null);

    // Assert
    assertEquals(false, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat one null property list as different")
  void shouldTreatOneNullPropertyListAsDifferent() {
    // Act
    Boolean result = invokePrivateMethod("havePropertiesChanged", (Object) null,
        List.of(property("language", "java")));

    // Assert
    assertEquals(true, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat property lists with different sizes as different")
  void shouldTreatPropertyListsWithDifferentSizesAsDifferent() {
    // Act
    Boolean result = invokePrivateMethod("havePropertiesChanged",
        List.of(property("language", "java")),
        List.of(property("language", "java"), property("tier", "backend")));

    // Assert
    assertEquals(true, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat property lists with different values as different")
  void shouldTreatPropertyListsWithDifferentValuesAsDifferent() {
    // Act
    Boolean result = invokePrivateMethod("havePropertiesChanged",
        List.of(property("language", "java")), List.of(new Property(null, "language", "kotlin")));

    // Assert
    assertEquals(true, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat two null relation lists as equal")
  void shouldTreatTwoNullRelationListsAsEqual() {
    // Act
    Boolean result = invokePrivateMethod("haveRelationsChanged", (Object) null, (Object) null);

    // Assert
    assertEquals(false, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat one null relation list as different")
  void shouldTreatOneNullRelationListAsDifferent() {
    // Act
    Boolean result = invokePrivateMethod("haveRelationsChanged", (Object) null,
        List.of(relation("owner", "team", "team-a")));

    // Assert
    assertEquals(true, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat relation lists with different sizes as different")
  void shouldTreatRelationListsWithDifferentSizesAsDifferent() {
    // Act
    Boolean result = invokePrivateMethod("haveRelationsChanged",
        List.of(relation("owner", "team", "team-a")),
        List.of(relation("owner", "team", "team-a"), relation("depends-on", "service", "svc-a")));

    // Assert
    assertEquals(true, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat relations with different target templates as different")
  void shouldTreatRelationsWithDifferentTargetTemplatesAsDifferent() {
    // Act
    Boolean result = invokePrivateMethod("haveRelationsChanged",
        List.of(relation("owner", "team", "team-a")),
        List.of(relation("owner", "group", "team-a")));

    // Assert
    assertEquals(true, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should treat relations with different target identifiers as different")
  void shouldTreatRelationsWithDifferentTargetIdentifiersAsDifferent() {
    // Act
    Boolean result = invokePrivateMethod("haveRelationsChanged",
        List.of(relation("owner", "team", "team-a")), List.of(relation("owner", "team", "team-b")));

    // Assert
    assertEquals(true, result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should delete entity directly when no parent relations exist")
  void shouldDeleteEntityDirectlyWhenNoParentRelationsExist() {
    // Arrange
    var targetEntity = entity("service", "catalog-api", "Catalog API");

    when(entityRepository.findByTemplateIdentifierAndIdentifier("service", "catalog-api"))
        .thenReturn(Optional.of(targetEntity));
    when(entityRepository.findEntitiesRelated("catalog-api")).thenReturn(List.of());

    // Act
    entityService.deleteEntity("service", "catalog-api");

    // Assert
    verify(entityRepository, never()).save(any());
    verify(entityTemplateService, never()).getEntityTemplateByIdentifier(anyString());
    verify(entityRepository).deleteByTemplateIdentifierAndIdentifier("service", "catalog-api");
  }

  @Test
  @DisplayName("Should return blocking relation names for required relations only")
  void shouldReturnBlockingRelationNamesForRequiredRelationsOnly() {
    // Arrange
    var linkedEntity = new Entity(UUID.randomUUID(), "application", "Commerce App", "commerce-app",
        List.of(),
        List.of(relation("owner", "team", "team-a"), relation("backup-owner", "team", "team-a"),
            relation("dependencies", "service", "team-a", "billing-service")));
    var parentTemplate = templateWithRelations("application",
        relationDefinition("owner", "team", true, false),
        relationDefinition("backup-owner", "team", true, true),
        relationDefinition("dependencies", "service", false, true));

    // Act
    String result = invokePrivateMethod("getBlockingRelationNames", linkedEntity, parentTemplate,
        "team-a");

    // Assert
    assertEquals("'owner', 'backup-owner'", result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should return no blocking relation names when deletion remains valid")
  void shouldReturnNoBlockingRelationNamesWhenDeletionRemainsValid() {
    // Arrange
    var linkedEntity = new Entity(UUID.randomUUID(), "application", "Commerce App", "commerce-app",
        List.of(), List.of(relation("dependencies", "service", "catalog-api", "billing-api")));
    var parentTemplate = templateWithRelations("application",
        relationDefinition("dependencies", "service", true, true));

    // Act
    String result = invokePrivateMethod("getBlockingRelationNames", linkedEntity, parentTemplate,
        "catalog-api");

    // Assert
    assertEquals("", result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should evaluate blocking relation combinations")
  void shouldEvaluateBlockingRelationCombinations() {
    // Arrange
    var parentTemplate = templateWithRelations("application",
        relationDefinition("owner", "team", true, false),
        relationDefinition("backup-owner", "team", false, false));

    // Act
    Boolean missingTarget = invokePrivateMethod("isBlockingRelation",
        relation("owner", "team", "team-b"), parentTemplate, "team-a");
    Boolean stillHasOtherTargets = invokePrivateMethod("isBlockingRelation",
        relation("owner", "team", "team-a", "team-b"), parentTemplate, "team-a");
    Boolean optionalRelation = invokePrivateMethod("isBlockingRelation",
        relation("backup-owner", "team", "team-a"), parentTemplate, "team-a");
    Boolean requiredRelation = invokePrivateMethod("isBlockingRelation",
        relation("owner", "team", "team-a"), parentTemplate, "team-a");

    // Assert
    assertEquals(false, missingTarget);
    assertEquals(false, stillHasOtherTargets);
    assertEquals(false, optionalRelation);
    assertEquals(true, requiredRelation);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should clean up relations by removing only matching targets")
  void shouldCleanUpRelationsByRemovingOnlyMatchingTargets() {
    // Arrange
    var dependencyRelation = relation("dependencies", "service", "catalog-api", "billing-api");
    var ownerRelation = relation("owner", "team", "team-a");
    var parent = new Entity(UUID.randomUUID(), "application", "Commerce App", "commerce-app",
        List.of(), List.of(dependencyRelation, ownerRelation));
    var parentTemplate = templateWithRelations("application",
        relationDefinition("dependencies", "service", false, true),
        relationDefinition("owner", "team", true, false));

    // Act
    Entity result = invokePrivateMethod("cleanUpRelations", parent, parentTemplate, "catalog-api");

    // Assert
    assertEquals(new Entity(parent.id(), "application", "Commerce App", "commerce-app", List.of(),
        List.of(relation(dependencyRelation.id(), "dependencies", "service", "billing-api"),
            ownerRelation)),
        result);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should keep existing relation when target identifier is not linked")
  void shouldKeepExistingRelationWhenTargetIdentifierIsNotLinked() {
    // Arrange
    var updatedRelations = new ArrayList<Relation>();
    var relation = relation("dependencies", "service", "billing-api");

    // Act
    invokePrivateMethod("retrieveAndCleanTargetEntitiesAgainstRelation",
        templateWithRelations("application",
            relationDefinition("dependencies", "service", false, true)),
        "catalog-api", relation, updatedRelations);

    // Assert
    assertEquals(List.of(relation), updatedRelations);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should clean related relation when target identifier matches")
  void shouldCleanRelatedRelationWhenTargetIdentifierMatches() {
    // Arrange
    var updatedRelations = new ArrayList<Relation>();
    var relation = relation("dependencies", "service", "catalog-api", "billing-api");

    // Act
    invokePrivateMethod("retrieveAndCleanTargetEntitiesAgainstRelation",
        templateWithRelations("application",
            relationDefinition("dependencies", "service", false, true)),
        "catalog-api", relation, updatedRelations);

    // Assert
    assertEquals(List.of(relation(relation.id(), "dependencies", "service", "billing-api")),
        updatedRelations);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should skip adding required relation when cleanup would leave it empty")
  void shouldSkipAddingRequiredRelationWhenCleanupWouldLeaveItEmpty() {
    // Arrange
    var updatedRelations = new ArrayList<Relation>();
    var relation = relation("owner", "team", "team-a");

    // Act
    invokePrivateMethod("cleanLinkedRelation",
        templateWithRelations("application", relationDefinition("owner", "team", true, false)),
        "team-a", relation, relation.targetEntityIdentifiers(), updatedRelations);

    // Assert
    assertEquals(List.of(), updatedRelations);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should add empty relation when optional relation becomes empty")
  void shouldAddEmptyRelationWhenOptionalRelationBecomesEmpty() {
    // Arrange
    var updatedRelations = new ArrayList<Relation>();
    var relation = relation("watcher", "team", "team-a");

    // Act
    invokePrivateMethod("cleanLinkedRelation",
        templateWithRelations("application", relationDefinition("watcher", "team", false, false)),
        "team-a", relation, relation.targetEntityIdentifiers(), updatedRelations);

    // Assert
    assertEquals(List.of(new Relation(relation.id(), "watcher", "team", List.of())),
        updatedRelations);
    verifyNoCollaboratorInteractions();
  }

  @Test
  @DisplayName("Should return null relation definition when template relation definitions are null")
  void shouldReturnNullRelationDefinitionWhenTemplateRelationDefinitionsAreNull() {
    // Arrange
    var template = mock(EntityTemplate.class);
    when(template.relationsDefinitions()).thenReturn(null);

    // Act
    RelationDefinition result = invokePrivateMethod("getRelationDefinition", template, "owner");

    // Assert
    assertEquals(null, result);
    verify(template).relationsDefinitions();
    verifyNoCollaboratorInteractions();
  }
}
