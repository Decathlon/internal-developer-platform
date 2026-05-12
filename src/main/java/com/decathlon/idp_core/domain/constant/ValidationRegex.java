package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationRegex {

    public static final String ENTITY_TEMPLATE_NAME_REGEX = "^[a-zA-Z0-9 _-]+$";
    public static final String FILTER_KEY_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9_-]*$";

}
