package com.decathlon.idp_core.domain.model.entity;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pure domain model representing a Relation between entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Relation {

    private UUID id;
    private String name;
    private String targetTemplateIdentifier;
    private List<String> targetEntityIdentifiers;
}
