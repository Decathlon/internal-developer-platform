package com.decathlon.idp_core.infrastructure.adapters.api.mapper.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.decathlon.idp_core.domain.model.entity.Property;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphNode;
import com.decathlon.idp_core.domain.model.entity_graph.EntityGraphRelation;
import com.decathlon.idp_core.domain.model.entity_template.EntityTemplate;
import com.decathlon.idp_core.domain.model.entity_template.PropertyDefinition;
import com.decathlon.idp_core.domain.model.enums.PropertyType;
import com.decathlon.idp_core.domain.service.entity_template.EntityTemplateService;

@DisplayName("EntityDtoOutFromEntityNodeMapper tests")
class EntityDtoOutFromEntityNodeMapperTest {

  private final EntityTemplateService entityTemplateService = mock(EntityTemplateService.class);

  private EntityDtoOutFromEntityNodeMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new EntityDtoOutFromEntityNodeMapper(entityTemplateService);
  }

  @Test
  @DisplayName("Uses each related entity template to convert its properties")
  void mapsRelatedPropertiesUsingRelatedTemplate() {
    var rootTemplate = new EntityTemplate(UUID.randomUUID(), "root-template", "Root", null,
        List.of(new PropertyDefinition(UUID.randomUUID(), "count", "Root count",
            PropertyType.STRING, false, null)),
        List.of());
    var relatedTemplate = new EntityTemplate(UUID.randomUUID(), "related-template", "Related", null,
        List.of(
            new PropertyDefinition(UUID.randomUUID(), "count", "Related count", PropertyType.NUMBER,
                false, null),
            new PropertyDefinition(UUID.randomUUID(), "enabled", "Related enabled",
                PropertyType.BOOLEAN, false, null)),
        List.of());
    when(entityTemplateService.getEntityTemplateByIdentifier("root-template"))
        .thenReturn(rootTemplate);
    when(entityTemplateService.getEntityTemplateByIdentifier("related-template"))
        .thenReturn(relatedTemplate);

    var relatedNode = new EntityGraphNode("related-template", "related-1", "Related entity",
        List.of(new Property(UUID.randomUUID(), "count", "42"),
            new Property(UUID.randomUUID(), "enabled", "true")),
        List.of(), List.of());
    var rootNode = new EntityGraphNode("root-template", "root-1", "Root entity",
        List.of(new Property(UUID.randomUUID(), "count", "root-value")),
        List.of(new EntityGraphRelation("uses", List.of(relatedNode))), List.of());

    var result = mapper.toDto(rootNode, "root-template", 1, true);

    assertThat(result.properties()).containsEntry("count", "root-value");
    assertThat(result.relations().get("uses").get(0).properties()).containsEntry("count", 42.0)
        .containsEntry("enabled", true);
  }
}