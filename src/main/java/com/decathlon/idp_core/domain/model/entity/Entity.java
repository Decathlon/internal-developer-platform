package com.decathlon.idp_core.domain.model.entity;

import static com.decathlon.idp_core.domain.constant.ValidationsMessages.TEMPLATE_IDENTIFIER_MANDATORY;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing an Entity in the system.
 * <p>
 * An Entity is a core domain object that is defined by a template identifier, a unique name, and a unique identifier. It can have a collection of properties and relations to other entities.
 * </p>
 * <ul>
 *   <li>Each entity is uniquely identified by its {@code id} (UUID) and {@code identifier} (String).</li>
 *   <li>Properties and relations are managed as separate collections and persisted via join tables.</li>
 *   <li>Validation constraints ensure required fields are present and unique where necessary.</li>
 * </ul>
 */
@jakarta.persistence.Entity
@Data
@Table
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entity {

        /**
         * Primary key for the entity (UUID).
         */
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        /**
         * Identifier of the template this entity is based on. Required field.
         */
        @Column(name = "template_identifier")
        @NotBlank(message = TEMPLATE_IDENTIFIER_MANDATORY)
        private String templateIdentifier;

        /**
         * Unique name of the entity. Required field.
         */
        @Column(unique = true)
        @NotBlank(message = "name is mandatory")
        private String name;

        /**
         * Unique business identifier for the entity.
         */
        @Column(unique = true)
        private String identifier;

        /**
         * List of properties associated with this entity.
         */
        @OneToMany(cascade = CascadeType.ALL)
        @JoinTable(name = "entity_properties", joinColumns = @JoinColumn(name = "entity_id"), inverseJoinColumns = @JoinColumn(name = "property_id"), uniqueConstraints = @UniqueConstraint(columnNames = {
                        "entity_id", "property_id" }), indexes = @Index(columnList = "entity_id"))
        private List<Property> properties;

        /**
         * List of relations to other entities.
         */
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinTable(name = "entity_relations", joinColumns = @JoinColumn(name = "entity_id"), inverseJoinColumns = @JoinColumn(name = "relation_id"), uniqueConstraints = @UniqueConstraint(columnNames = {
                        "entity_id", "relation_id" }), indexes = @Index(columnList = "entity_id"))
        private List<Relation> relations;

}
