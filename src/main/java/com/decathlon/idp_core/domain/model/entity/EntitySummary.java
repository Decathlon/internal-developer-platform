package com.decathlon.idp_core.domain.model.entity;

import lombok.Builder;

/**
 * Record representing a summary view of an Entity.
 * <p>
 *   Contains the minimal identifying information for an entity, including:
 *   <ul>
 *     <li><b>identifier</b>: the unique business identifier of the entity</li>
 *     <li><b>name</b>: the display name of the entity</li>
 *     <li><b>templateIdentifier</b>: the identifier of the template this entity is based on</li>
 *   </ul>
 * Used for lightweight projections and summary responses in APIs.
 * </p>
 *
 */
@Builder
public record EntitySummary(String identifier, String name, String templateIdentifier) {}
