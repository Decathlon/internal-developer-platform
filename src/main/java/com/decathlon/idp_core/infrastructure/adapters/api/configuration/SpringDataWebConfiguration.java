
package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;


/**
 * Spring Data Web configuration for the IDP Core application.
 * <p>
 * Enables {@code VIA_DTO} page serialization mode so that {@code Page<T>} REST responses
 * are serialized as a clean DTO structure:
 * <pre>
 * {
 *   "content": [...],
 *   "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 }
 * }
 * </pre>
 * instead of the default HATEOAS-style format with {@code _links} and {@code _embedded}.
 * </p>
 */
@Configuration
@EnableSpringDataWebSupport(
    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SpringDataWebConfiguration {

}
