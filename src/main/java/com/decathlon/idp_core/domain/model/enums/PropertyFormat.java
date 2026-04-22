package com.decathlon.idp_core.domain.model.enums;

import com.decathlon.idp_core.domain.model.entity_template.PropertyRules;

/// Business format constraints for property validation in [PropertyRules].
///
/// Defines standardized data formats that properties must conform to beyond basic
/// type checking. These formats represent common business data patterns that require
/// specific validation logic.
///
/// **Business purpose:**
/// - Ensures data quality for business-critical formats
/// - Provides consistent validation across the domain
/// - Supports integration with external systems requiring specific formats
public enum PropertyFormat {
    URL,
    EMAIL
}
