package com.decathlon.idp_core.domain.port;


import com.decathlon.idp_core.domain.exception.EntityDynamicMappingException;
import com.decathlon.idp_core.domain.model.entity.Entity;
import com.decathlon.idp_core.domain.model.entity_mapping.EntityDynamicMappingConfiguration;

public interface EntityDynamicMapper {
    

    /**
     * Transforms raw JSON source data into a standardized SoftwareEntity.
     *
     * @param rawJson The source payload (e.g., from SonarQube or GitHub).
     * @param config  The DSL configuration containing mapping rules and filters.
     * @return A SoftwareEntity, or null if the config filter excludes the data.
     * @throws EntityDynamicMappingException if mapping logic fails or results in invalid data.
     */
    Entity map(String rawJson, EntityDynamicMappingConfiguration config);
}
