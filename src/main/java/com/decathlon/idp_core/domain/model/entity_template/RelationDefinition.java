package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_NAME_MANDATORY_SIMPLE;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pure domain model representing a RelationDefinition within an EntityTemplate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationDefinition {

    private UUID id;

    @NotBlank(message = RELATION_NAME_MANDATORY_SIMPLE)
    private String name;

    @NotBlank(message = RELATION_TARGET_IDENTIFIER_MANDATORY_SIMPLE)
    private String targetEntityIdentifier;

    @Builder.Default
    private boolean required = false;

    @Builder.Default
    private boolean toMany = false;
}
