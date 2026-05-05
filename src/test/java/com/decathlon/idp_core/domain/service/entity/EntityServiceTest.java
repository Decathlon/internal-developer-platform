package com.decathlon.idp_core.domain.service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.port.EntityTemplateRepositoryPort;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateValidationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityService Tests")
class EntityServiceTest {

    @Mock
    private EntityRepositoryPort entityRepository;

    @Mock
    private EntityTemplateRepositoryPort entityTemplateRepository;

    @Mock
    private EntityValidationService entityValidationService;

    @Mock
    private EntityTemplateValidationService entityTemplateValidationService;

    @InjectMocks
    private EntityService entityService;

    @Test
    @DisplayName("Should return entities page by template identifier")
    void shouldReturnEntitiesByTemplateIdentifier() {
        var pageable = Pageable.ofSize(10);
        var entity = entity("template-a", "entity-a", "Entity A");
        var page = new PageImpl<>(List.of(entity));

        when(entityRepository.findByTemplateIdentifier("template-a", pageable)).thenReturn(Optional.of(page));

        var result = entityService.getEntitiesByTemplateIdentifier(pageable, "template-a");

        assertSame(page, result);
        verify(entityRepository).findByTemplateIdentifier("template-a", pageable);
    }

    @Test
    @DisplayName("Should throw when template has no entities page")
    void shouldThrowWhenTemplatePageNotFound() {
        var pageable = Pageable.ofSize(10);
        when(entityRepository.findByTemplateIdentifier("missing-template", pageable)).thenReturn(Optional.empty());

        assertThrows(EntityTemplateNotFoundException.class,
                () -> entityService.getEntitiesByTemplateIdentifier(pageable, "missing-template"));
    }

    @Test
    @DisplayName("Should return entity summaries by identifiers")
    void shouldReturnEntitySummariesByIdentifiers() {
        var summaries = List.of(new EntitySummary("service-a", "Service A", "web-service"));
        when(entityRepository.findByIdentifierIn(List.of("service-a"))).thenReturn(summaries);

        var result = entityService.getEntitiesSummariesByIndentifiers(List.of("service-a"));

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
        verify(entityTemplateValidationService).checkTemplateExists("web-service");
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
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(), List.of());
        when(entityTemplateRepository.findByIdentifier("web-service")).thenReturn(Optional.of(template));
        when(entityRepository.save(entity)).thenReturn(entity);

        var result = entityService.createEntity(entity);

        assertSame(entity, result);

        InOrder inOrder = inOrder(entityTemplateRepository, entityValidationService, entityRepository);
        inOrder.verify(entityTemplateRepository).findByIdentifier("web-service");
        inOrder.verify(entityValidationService).checkUniqueness(entity);
        inOrder.verify(entityValidationService).validateEntity(entity, template);
        inOrder.verify(entityRepository).save(entity);
        verifyNoInteractions(entityTemplateValidationService);
    }

    @Test
    @DisplayName("Should not save when entity already exists")
    void shouldNotSaveWhenEntityAlreadyExists() {
        var entity = entity("web-service", "catalog-api", "Catalog API");
        var template = new EntityTemplate(UUID.randomUUID(), "web-service", "Web Service", "desc", List.of(), List.of());
        var alreadyExists = new EntityAlreadyExistsException("web-service", "catalog-api");

        when(entityTemplateRepository.findByIdentifier("web-service")).thenReturn(Optional.of(template));
        org.mockito.Mockito.doThrow(alreadyExists).when(entityValidationService).checkUniqueness(entity);

        assertThrows(EntityAlreadyExistsException.class, () -> entityService.createEntity(entity));

        verify(entityTemplateRepository).findByIdentifier("web-service");
        verify(entityValidationService).checkUniqueness(entity);
        verifyNoMoreInteractions(entityRepository);
    }

    @Test
    @DisplayName("Should stop immediately when template does not exist")
    void shouldStopWhenTemplateDoesNotExistOnCreate() {
        var entity = entity("missing-template", "catalog-api", "Catalog API");

        when(entityTemplateRepository.findByIdentifier("missing-template")).thenReturn(Optional.empty());

        assertThrows(EntityTemplateNotFoundException.class, () -> entityService.createEntity(entity));

        verify(entityTemplateRepository).findByIdentifier("missing-template");
        verifyNoInteractions(entityValidationService);
        verifyNoMoreInteractions(entityRepository);
    }

    private Entity entity(String templateIdentifier, String identifier, String name) {
        return new Entity(UUID.randomUUID(), templateIdentifier, name, identifier, List.of(), List.of());
    }
}
