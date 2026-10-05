package com.decathlon.idp_core.infrastructure.adapters.persistence.model.entity;
import java.util.UUID;
public interface EntityGraphJsonProjection {
  UUID getId();
  String getIdentifier();
  String getName();
  String getTemplateIdentifier();
  String getPropertiesJson();
  String getRelationsJson();
}