package com.decathlon.idp_core.domain.port;

import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMapping;

public interface EntityDynamicMapperValidator {

    void validate(EntityDynamicMapping mapping);
}
