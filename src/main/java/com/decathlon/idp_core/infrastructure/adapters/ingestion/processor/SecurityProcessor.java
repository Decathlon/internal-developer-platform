package com.decathlon.idp_core.infrastructure.adapters.ingestion.processor;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SecurityProcessor {
  public boolean validate() {
    return true;
  }
}
