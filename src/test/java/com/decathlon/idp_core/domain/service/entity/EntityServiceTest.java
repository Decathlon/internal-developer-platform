package com.decathlon.idp_core.domain.service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

import com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException;
import com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException;
import com.decathlon.idp_core.domain.exception.entity.EntityValidationException;
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.constant.ValidationMessages;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntityFilter;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.EntityQueryParserService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

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
    private EntityQueryParserService entityQueryParserService;

    @InjectMocks
    private EntityService entityService;

    @Test
    @DisplayName("Should return entities page by template identifier")
    void shouldReturnEntitiesByTemplateIdentifier() {
        var pageable = Pageable.ofSize(10);
        var entity = entity("template-a", "entity-a", "Entity A");
        var page = new PageImpl<>(List.of(entity));
        var template = new EntityTemplate(UUID.randomUUID(), "template-a", "Template A", "desc", List.of(),
                List.of());

        when(entityTemplateService.getEntityTemplateByIdentifier("template-a")).thenReturn(template);
        when(entityRepository.findByTemplateIdentifierWithFilter("template-a", EntityFilter.empty(), pageable))
                .thenReturn(page);

        var result = entityService.getEntitiesByTemplateIdentifier(pageable, "template-a", null);

        assertSame(page, result);
        verify(entityTemplateService).getEntityTemplateByIdentifier("template-a");
        verify(entityQueryParserService).validateFilterPropertyTypes(EntityFilter.empty(), template);
        verify(entityRepository).findByTemplateIdentifierWithFilter("template-a", EntityFilter.empty(), pageable);
    }

    @Test
    @DisplayName("Should return entity summaries by identifiers")
    void shouldReturnEntitySummariesByIdentifiers() {
        var summaries = List.of(new EntitySummary("service-a", "Service A", "web-service"));
        when(entityRepository.findByIdentifierIn(List.of("service-a"))).thenReturn(summaries);

        var result = entityService.getEntitiesSummariesByIdentifiers(List.of("service-a"));

        assertEquals(summaries, result);
        verify(entityRepository).findByIdentifierIn(List.of("service-a"));
    }

    @Test
    @DisplayName("Should return entity by template and identifier")
    void shouldReturnEntityByTemplateAndIdentifier() {
        var entity = entity("web-service", "catalog-api", "Catalog API");
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "catalog-api"))
                .thenReturn(Optional.of(entity));

        var result = entityService.getEntityByTemplateIdentifierAndIdentifier("web-service", "catalog-api");

        assertSame(entity, result);
        verify(entityTemplateValidationService).validateTemplateExists("web-service");
        verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "catalog-api");
    }

    @Test
    @DisplayName("Should throw when entity is not found for template")
    void shouldThrowWhenEntityNotFoundByTemplateAndIdentifier() {
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "missing-entity"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> entityService.getEntityByTemplateIdentifierAndIdentifier("web-service", "missing-entity"));
    }

    @Test
    @DisplayName("Should create entity when validations pass")
    void shouldCreateEntityWhenValidationsPass() {
        var entity = entity("web-service", "catalog-api", "Catalog API");
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(),
                List.of());
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
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(),
                List.of());
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
        var existing = new Entity(UUID.randomUUID(), "web-service", "Web API 2", "web-api-2", List.of(), List.of());
        var payload = new Entity(null, "web-service", "Web API 2 Updated", "web-api-2", List.of(), List.of());
        var expectedSaved = new Entity(existing.id(), "web-service", "Web API 2 Updated", "web-api-2", List.of(), List.of());
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(), List.of());

        when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "web-api-2")).thenReturn(Optional.of(existing));
        when(entityRepository.save(expectedSaved)).thenReturn(expectedSaved);

        var result = entityService.updateEntity("web-service", "web-api-2", payload);

        assertSame(expectedSaved, result);
        InOrder inOrder = inOrder(entityTemplateService, entityRepository, entityValidationService, entityRepository);
        inOrder.verify(entityTemplateService).getEntityTemplateByIdentifier("web-service");
        inOrder.verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "web-api-2");
        inOrder.verify(entityValidationService).validateForUpdate(expectedSaved, template);
        inOrder.verify(entityRepository).save(expectedSaved);
    }

    @Test
    @DisplayName("Should throw when updating non-existing entity")
    void shouldThrowWhenUpdatingNonExistingEntity() {
        var payload = new Entity(null, "web-service", "Web API 2 Updated", "web-api-2", List.of(), List.of());
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(), List.of());

        when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "web-api-2")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> entityService.updateEntity("web-service", "web-api-2", payload));

        verify(entityTemplateService).getEntityTemplateByIdentifier("web-service");
        verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "web-api-2");
        verifyNoMoreInteractions(entityRepository);
    }

    @Test
    @DisplayName("Should reject update when path identifier and body identifier differ")
    void shouldRejectUpdateWhenPathAndBodyIdentifierDiffer() {
        var existing = new Entity(UUID.randomUUID(), "web-service", "Web API 2", "web-api-2", List.of(), List.of());
        var payload = new Entity(null, "web-service", "Web API 2 Updated", "different-id", List.of(), List.of());
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(), List.of());

        when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "web-api-2")).thenReturn(Optional.of(existing));

        assertThrows(EntityValidationException.class,
                () -> entityService.updateEntity("web-service", "web-api-2", payload));

        verify(entityTemplateService).getEntityTemplateByIdentifier("web-service");
        verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "web-api-2");
        verifyNoInteractions(entityValidationService);
    }

    @Test
    @DisplayName("Should propagate two validation errors when update payload violates template constraints")
    void shouldPropagateTwoValidationErrorsWhenUpdatingInvalidEntity() {
        var existing = new Entity(UUID.randomUUID(), "web-service", "Web API 2", "web-api-2", List.of(), List.of());
        var payload = new Entity(null, "web-service", "Web API 2 Updated", "web-api-2", List.of(), List.of());
        var expectedToValidate = new Entity(existing.id(), "web-service", "Web API 2 Updated", "web-api-2", List.of(), List.of());
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(), List.of());
        var validationErrors = List.of(
                ValidationMessages.PROPERTY_NOT_DEFINED_IN_TEMPLATE.formatted("status", "web-service"),
                ValidationMessages.RELATION_TARGET_ENTITY_NOT_FOUND.formatted("child_of", "missing-platform")
        );

        when(entityTemplateService.getEntityTemplateByIdentifier("web-service")).thenReturn(template);
        when(entityRepository.findByTemplateIdentifierAndIdentifier("web-service", "web-api-2")).thenReturn(Optional.of(existing));
        doThrow(new EntityValidationException(validationErrors))
                .when(entityValidationService).validateForUpdate(expectedToValidate, template);

        var thrown = assertThrows(EntityValidationException.class,
                () -> entityService.updateEntity("web-service", "web-api-2", payload));

        assertEquals(validationErrors, thrown.getViolations());
        verify(entityValidationService).validateForUpdate(expectedToValidate, template);
        verify(entityRepository).findByTemplateIdentifierAndIdentifier("web-service", "web-api-2");
        verifyNoMoreInteractions(entityRepository);
    }

    private Entity entity(String templateIdentifier, String identifier, String name) {
        return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(), List.of());
    }
}
