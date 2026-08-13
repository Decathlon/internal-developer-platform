package com.decathlon.idp_core.infrastructure.adapters.api.principal.strategies;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrincipalStrategiesConstants {
  public static final String CLIENT_ID = "client_id";
  public static final String SERVICE_NAME = "service_name";
  public static final String SUB = "sub";
  public static final String NAME = "name";
  public static final String EMAIL = "email";
  public static final String GROUPS = "groups";
  public final static String GRANT_TYPE = "grant_type";
  public final static String GTY = "gty";
  public final static String PREFERRED_USERNAME = "preferred_username";
  public final static String ORIGIN = "origin";
  public final static String CLIENT_CREDENTIALS = "client_credentials";
}
