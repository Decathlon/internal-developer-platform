package com.decathlon.idp_core.domain.service.relation;

import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_NOT_DEFINED_IN_TEMPLATE;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_REQUIRED_MISSING;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TARGET_ENTITY_NOT_FOUND;
import static com.decathlon.idp_core.domain.constant.ValidationMessages.RELATION_TOO_MANY_TARGETS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.decathlon.idp_core.domain.model.entity.EntitySummary;
import com.decathlon.idp_core.domain.model.entity.Relation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.RelationDefinition;
import com.decathlon.idp_core.domain.port.EntityRepositoryPort;
import com.decathlon.idp_core.domain.service.entity.Violations;

@DisplayName("RelationValidationService Tests")
@ExtendWith(MockitoExtension.class)
class RelationValidationServiceTest {

  @Mock
  private EntityRepositoryPort entityRepository;

  private RelationValidationService service;

  @BeforeEach
  void setUp() {
    service = new RelationValidationService(entityRepository);
  }

  private void mockExistingEntities(String... identifiers) {
    var summaries = Arrays.stream(identifiers).map(id -> new EntitySummary(id, "Name", "template"))
        .toList();
    when(entityRepository.findByIdentifierIn(any())).thenReturn(summaries);
  }

  @Test
  @DisplayName("Should pass all checks cleanly when relations map exactly to definitions")
  void shouldPassCleanlyOnValidEntity() {
    mockExistingEntities("team-a", "service-x", "service-y");
    var definition1 = definition("owned-by", true, false);
    var definition2 = definition("depends-on", false, true);
    var template = template("system-template", List.of(definition1, definition2));

    var relation1 = relation("owned-by", List.of("team-a"));
    var relation2 = relation("depends-on", List.of("service-x", "service-y"));

    var violations = mock(Violations.class);

    service.validateRelationsAgainstTemplate(template, List.of(relation1, relation2), violations);

    verifyNoInteractions(violations);
  }

  private EntityTemplate template(String identifier, List<RelationDefinition> relationDefinitions) {
    return new EntityTemplate(UUID.randomUUID(), identifier, "Template Name", "Description",
        List.of(), relationDefinitions);
  }

  private RelationDefinition definition(String name, boolean required, boolean toMany) {
    return new RelationDefinition(UUID.randomUUID(), name, "targetType", required, toMany);
  }

  private Relation relation(String name, List<String> targets) {
    return new Relation(UUID.randomUUID(), name, "targetType", targets);
  }

  @Nested
  @DisplayName("Relation Existence Checks")
  class ExistenceTests {

    @Test
    @DisplayName("Should report violation when relation is not defined in the template")
    void shouldReportViolationWhenRelationNotDefined() {
      var template = template("system-template", List.of());
      var relation = relation("unknown-relation", List.of("target-1"));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(relation), violations);

      verify(violations).add(RELATION_NOT_DEFINED_IN_TEMPLATE, "unknown-relation",
          "system-template");
    }

    @Test
    @DisplayName("Should handle missing definition lists and relation lists gracefully")
    void shouldHandleNullListsGracefully() {
      var template = new EntityTemplate(UUID.randomUUID(), "system-template", "System", "desc",
          List.of(), null);
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, null, violations);

      verifyNoInteractions(violations);
    }

    @Test
    @DisplayName("Should report violation when relation target entity does not exist")
    void shouldReportViolationWhenRelationTargetEntityDoesNotExist() {
      mockExistingEntities("existing-team");
      var definition = definition("owned-by", true, false);
      var template = template("system-template", List.of(definition));
      var relation = relation("owned-by", List.of("missing-team"));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(relation), violations);

      verify(violations).add(RELATION_TARGET_ENTITY_NOT_FOUND, "owned-by", "missing-team");
    }
  }

  @Nested
  @DisplayName("Relation Requirement Checks")
  class RequirementTests {

    @Test
    @DisplayName("Should report violation when required relation is missing completely")
    void shouldReportViolationWhenRequiredRelationMissing() {
      var definition = definition("owned-by", true, false);
      var template = template("system-template", List.of(definition));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(), violations);

      verify(violations).add(RELATION_REQUIRED_MISSING, "owned-by", "system-template");
    }

    @Test
    @DisplayName("Should report violation when required relation is provided but target list is empty")
    void shouldReportViolationWhenRequiredRelationHasEmptyTargets() {
      var definition = definition("owned-by", true, false);
      var template = template("system-template", List.of(definition));
      var relation = relation("owned-by", List.of());
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(relation), violations);

      verify(violations).add(RELATION_REQUIRED_MISSING, "owned-by", "system-template");
    }

    @Test
    @DisplayName("Should report violation when required relation only has blank targets")
    void shouldReportViolationWhenRequiredRelationHasOnlyBlankTargets() {
      var definition = definition("owned-by", true, false);
      var template = template("system-template", List.of(definition));
      var relation = relation("owned-by", List.of("", "   "));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(relation), violations);

      verify(violations).add(RELATION_REQUIRED_MISSING, "owned-by", "system-template");
    }

    @Test
    @DisplayName("Should not report violation when an optional relation is omitted")
    void shouldNotReportViolationWhenOptionalRelationOmitted() {
      var definition = definition("depends-on", false, true);
      var template = template("system-template", List.of(definition));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(), violations);

      verifyNoInteractions(violations);
    }
  }

  @Nested
  @DisplayName("Relation Cardinality Checks")
  class CardinalityTests {

    @Test
    @DisplayName("Should report violation when a non-toMany relation has multiple valid targets")
    void shouldReportViolationForMultipleTargetsOnSingleRelation() {
      mockExistingEntities("team-a", "team-b");
      var definition = definition("owned-by", true, false);
      var template = template("system-template", List.of(definition));
      var relation = relation("owned-by", List.of("team-a", "team-b"));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(relation), violations);

      verify(violations).add(RELATION_TOO_MANY_TARGETS, "owned-by", "system-template");
    }

    @Test
    @DisplayName("Should not report violation for multiple targets if toMany is true")
    void shouldNotReportViolationForMultipleTargetsWhenToManyIsTrue() {
      mockExistingEntities("service-a", "service-b", "service-c");
      var definition = definition("depends-on", false, true);
      var template = template("system-template", List.of(definition));
      var relation = relation("depends-on", List.of("service-a", "service-b", "service-c"));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(relation), violations);

      verifyNoInteractions(violations);
    }

    @Test
    @DisplayName("Should ignore blank targets when checking cardinality constraints")
    void shouldIgnoreBlankTargetsForCardinality() {
      mockExistingEntities("team-a");
      var definition = definition("owned-by", true, false);
      var template = template("system-template", List.of(definition));
      var relation = relation("owned-by", List.of("team-a", "   ", ""));
      var violations = mock(Violations.class);

      service.validateRelationsAgainstTemplate(template, List.of(relation), violations);

      verifyNoInteractions(violations);
    }
  }
}
