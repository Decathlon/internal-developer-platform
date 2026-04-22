package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;

import java.util.UUID;

import com.decathlon.idp_core.domain.model.enums.PropertyFormat;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "property_rules")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRulesJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private PropertyFormat format;

    private String[] enumValues;
    private String regex;
    private Integer maxLength;
    private Integer minLength;
    private Integer maxValue;
    private Integer minValue;
}
