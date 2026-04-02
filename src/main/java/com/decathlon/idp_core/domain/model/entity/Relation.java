package com.decathlon.idp_core.domain.model.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a relation between entities.
 * <p>
 * A Relation defines a named connection from a source entity to one or more target entities, optionally filtered by target template.
 * </p>
 * <ul>
 *   <li>Each relation has a unique identifier ({@code id}) and a required name.</li>
 *   <li>The {@code targetTemplateIdentifier} can be used to restrict the type of target entities.</li>
 *   <li>{@code targetEntityIdentifiers} holds the ordered list of identifiers for the target entities.</li>
 * </ul>
 */
@Entity
@Data
@Table
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Relation {

    /**
     * Primary key for the relation (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Name of the relation. Required field.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Template identifier for the target entities. Required field.
     */
    @Column(name = "target_template_identifier", nullable = false)
    private String targetTemplateIdentifier;

    /**
     * Ordered list of target entity identifiers for this relation.
     */
    @ElementCollection
    @CollectionTable(name = "relation_target_entities", joinColumns = @JoinColumn(name = "relation_id"), indexes = @Index(columnList = "relation_id"))
    @Column(name = "target_entity_identifier")
    private List<String> targetEntityIdentifiers;
}
