package com.decathlon.idp_core.infrastructure.adapters.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ErrorResponse {
  private String error;
  private String errorDescription;
}
