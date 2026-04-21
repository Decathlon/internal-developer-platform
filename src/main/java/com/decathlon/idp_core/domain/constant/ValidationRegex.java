package com.decathlon.idp_core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationRegex {

    // Entity Template name regex
    public static final String TEMPLATE_NAME_REGEX = "^[a-zA-Z0-9 _-]+$";

}
