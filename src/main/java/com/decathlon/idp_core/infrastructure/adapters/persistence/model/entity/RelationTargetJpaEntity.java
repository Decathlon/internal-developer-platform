package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.hibernate.envers.Audited;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Audited
@NoArgsConstructor
@AllArgsConstructor
public class RelationTargetJpaEntity {

  @Column(name = "target_entity_uuid", nullable = false)
  private UUID targetEntityUuid;

  @Column(name = "target_entity_identifier", nullable = false)
  private String targetEntityIdentifier;

  // Overriding equals and hashCode is best practice for ElementCollections
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RelationTargetJpaEntity that = (RelationTargetJpaEntity) o;
    return Objects.equals(targetEntityUuid, that.targetEntityUuid)
        && Objects.equals(targetEntityIdentifier, that.targetEntityIdentifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetEntityUuid, targetEntityIdentifier);
  }
}
