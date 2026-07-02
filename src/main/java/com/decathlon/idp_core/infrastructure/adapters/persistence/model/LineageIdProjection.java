package com.decathlon.idp_core.infrastructure.adapters.persistence.model;

import java.util.UUID;

public interface LineageIdProjection {
  UUID getId();
  Long getTotalCount();
}
