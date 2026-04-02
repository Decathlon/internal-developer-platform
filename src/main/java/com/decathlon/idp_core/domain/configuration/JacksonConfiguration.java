
package com.decathlon.idp_core.domain.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Jackson configuration class for the IDP Core application.
 *
 * This configuration enables Spring Data Web support with specific page serialization settings.
 * The page serialization mode is set to VIA_DTO, which means that Spring Data Page objects
 * will be serialized using Data Transfer Objects instead of the default serialization format.
 *
 * This approach provides better control over the JSON structure of paginated responses
 * and ensures consistent API response formats across the application.
 */

@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class JacksonConfiguration {

}
