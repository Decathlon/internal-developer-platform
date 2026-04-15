
package com.decathlon.idp_core.infrastructure.adapters.api.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;


/// Spring Data Web configuration optimizing REST API response serialization.
///
/// **Infrastructure rationale:** Configures clean DTO-style pagination responses instead
/// of HATEOAS format. API consumers prefer simple JSON structure over complex hypermedia.
///
/// **Serialization format:**
/// ```
/// {
///   "content": [...],
///   "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 }
/// }
/// ```
///
/// **Alternative avoided:** Default HATEOAS format includes `_links` and `_embedded`
/// properties that increase response size and complexity for simple API consumption.
@Configuration
@EnableSpringDataWebSupport(
    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SpringDataWebConfiguration {

}
