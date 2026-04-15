package com.decathlon.idp_core.domain.model.entity_template;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_DESCRIPTION_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_NAME_MANDATORY;
import static com.decathlon.idp_core.domain.constant.ValidationsMessages.PROPERTY_TYPE_MANDATORY;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pure domain model representing a PropertyDefinition within an EntityTemplate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDefinition {

    private UUID id;

    @NotBlank(message = PROPERTY_NAME_MANDATORY)
    private String name;

    @NotBlank(message = PROPERTY_DESCRIPTION_MANDATORY)
    private String description;

    @NotNull(message = PROPERTY_TYPE_MANDATORY)
    private PropertyType type;

    @Builder.Default
    private boolean required = false;

    private PropertyRules rules;
}
