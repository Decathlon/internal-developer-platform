package com.decathlon.idp_core.domain.model.entity;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pure domain model representing an Entity in the system.
 * <p>
 * Free of persistence annotations. JPA mapping is in the infrastructure layer.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entity {

        private UUID id;

        @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
        private String templateIdentifier;

        private String name;

        private String identifier;

        private List<Property> properties;

        private List<Relation> relations;
}
