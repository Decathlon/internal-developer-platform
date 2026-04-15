package com.decathlon.idp_core.domain.model.entity_template;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pure domain model representing validation rules for a PropertyDefinition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRules {

    private UUID id;
    private PropertyFormat format;
    private String[] enumValues;
    private String regex;
    private Integer maxLength;
    private Integer minLength;
    private Integer maxValue;
    private Integer minValue;
}
