package com.decathlon.idp_core.domain.model.entity;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pure domain model representing a Property of an Entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    private UUID id;

    @NotBlank(message = "Property name is mandatory")
    private String name;

    @NotBlank(message = "Property value is mandatory")
    private String value;
}
