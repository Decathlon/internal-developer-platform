package com.decathlon.idp_core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.Map;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/// General maintainability guardrails that keep the codebase consistent, predictable and easy to evolve
/// See `.github/instructions/{java,springboot,domain}.instructions.md` for more details.
///
/// They cover :
/// - Dependency-injection best practices
/// - Logging discipline
/// - Exception handling
/// - Project naming and placement conventions
@AnalyzeClasses(packages = MaintainabilityArchitectureTest.BASE_PACKAGE, importOptions = DoNotIncludeTests.class)
class MaintainabilityArchitectureTest {

  static final String BASE_PACKAGE = "com.decathlon.idp_core";

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String EXCEPTION_PACKAGE = "..domain.exception..";

  // ---------------------------------------------------------------------------------------
  // 1. Cyclic dependencies
  // ---------------------------------------------------------------------------------------

  /// No dependency cycles between the top-level slices of the application.
  @ArchTest
  static final ArchRule FREE_OF_CYCLES = slices().matching("com.decathlon.idp_core.(*)..").should()
      .beFreeOfCycles()
      .because("package cycles are the primary symptom of architecture erosion (ADR-0002)");

  /// No dependency cycles between domain sub-packages (model, service, port,
  /// ...).
  @ArchTest
  static final ArchRule DOMAIN_FREE_OF_CYCLES = slices()
      .matching("com.decathlon.idp_core.domain.(*)..").should().beFreeOfCycles()
      .because("the domain must stay internally untangled to remain easy to reason about");

  /// No dependency cycles between infrastructure adapters (api, persistence,
  /// formula, ...).
  @ArchTest
  static final ArchRule ADAPTERS_FREE_OF_CYCLES = slices()
      .matching("com.decathlon.idp_core.infrastructure.adapters.(*)..").should().beFreeOfCycles()
      .because("adapters must remain independent plugs that can evolve in isolation");

  // ---------------------------------------------------------------------------------------
  // 2. Dependency-injection & logging
  // ---------------------------------------------------------------------------------------

  /// Beans must use constructor injection; field/setter injection hides
  /// dependencies and makes classes harder to test (springboot.instructions.md)
  @ArchTest
  static final ArchRule NO_FIELD_INJECTION = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
      .because("constructor injection is mandatory; field injection hides dependencies");

  /// Production code must log through SLF4J, never the console
  @ArchTest
  static final ArchRule NO_STANDARD_STREAMS = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
      .because("use the SLF4J logger instead of System.out / System.err / printStackTrace");

  /// Production code must log through SLF4J, never `java.util.logging`
  @ArchTest
  static final ArchRule NO_JAVA_UTIL_LOGGING = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
      .because("SLF4J (with Logback) is the single logging facade used across the project");

  /// The domain is pure business logic and must not perform any logging; logging
  /// is an infrastructure concern (domain.instructions.md)
  @ArchTest
  static final ArchRule DOMAIN_MUST_NOT_LOG = noClasses().that().resideInAPackage(DOMAIN_PACKAGE)
      .should().dependOnClassesThat()
      .resideInAnyPackage("org.slf4j..", "ch.qos.logback..", "org.apache.logging..",
          "org.apache.commons.logging..")
      .because("the domain must stay free of logging frameworks; logging belongs to adapters");

  // ---------------------------------------------------------------------------------------
  // 3. Exception handling
  // ---------------------------------------------------------------------------------------

  /// Every runtime exception type must be named `*Exception`
  @ArchTest
  static final ArchRule RUNTIME_EXCEPTIONS_ARE_NAMED_EXCEPTION = classes().that()
      .areAssignableTo(RuntimeException.class).should().haveSimpleNameEndingWith("Exception")
      .because("the *Exception suffix is the project convention for throwable types");

  /// Every `*Exception` type must live in the domain exception package
  @ArchTest
  static final ArchRule EXCEPTIONS_LIVE_IN_DOMAIN_EXCEPTION_PACKAGE = classes().that()
      .haveSimpleNameEndingWith("Exception").should().resideInAPackage(EXCEPTION_PACKAGE)
      .because("domain exceptions are grouped under domain.exception");

  // ---------------------------------------------------------------------------------------
  // 4. Naming & placement conventions
  // ---------------------------------------------------------------------------------------

  /// `*Service` classes are domain services and must live in the domain service
  /// package
  @ArchTest
  static final ArchRule SERVICES_LIVE_IN_DOMAIN_SERVICE_PACKAGE = classes().that()
      .haveSimpleNameEndingWith("Service").should().resideInAPackage("..domain.service..")
      .because("business services belong to the domain service layer");

  /// `*Controller` classes are driving REST adapters and must live in the API
  /// package
  @ArchTest
  static final ArchRule CONTROLLERS_LIVE_IN_API_CONTROLLER_PACKAGE = classes().that()
      .haveSimpleNameEndingWith("Controller").should()
      .resideInAPackage("..adapters.api.controller..")
      .because("REST controllers are a driving adapter and belong under adapters.api.controller");

  /// `*Mapper` classes translate between layers and must live in a `mapper`
  /// package next to their adapter
  @ArchTest
  static final ArchRule MAPPERS_LIVE_IN_MAPPER_PACKAGE = classes().that()
      .haveSimpleNameEndingWith("Mapper").should().resideInAPackage("..mapper..")
      .because("mappers are grouped in a dedicated mapper package per adapter");

  // ---------------------------------------------------------------------------------------
  // 5. Exception registration
  // ---------------------------------------------------------------------------------------

  /// Every domain exception must be registered in
  /// `ApiExceptionHandler.STATUS_BY_EXCEPTION`
  /// so that it maps to an HTTP status and does not silently degrade to 500.
  @ArchTest
  static void allDomainExceptionsAreRegistered(com.tngtech.archunit.core.domain.JavaClasses classes)
      throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
    java.util.Objects.requireNonNull(classes, "ArchUnit provided classes must not be null");
    var classLoader = Thread.currentThread().getContextClassLoader();

    // List of known domain exception classes that should be registered
    String[] exceptionClassNames = {
        "com.decathlon.idp_core.domain.exception.entity.EntityAlreadyExistsException",
        "com.decathlon.idp_core.domain.exception.entity.EntityDeletionBlockedException",
        "com.decathlon.idp_core.domain.exception.entity.EntityNotFoundException",
        "com.decathlon.idp_core.domain.exception.entity.EntityValidationException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyExistsException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingAlreadyInUseException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingConfigurationException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoPropertiesException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingHasNoRelationsException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingJsltErrorException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.EntityDynamicMappingNotFoundException",
        "com.decathlon.idp_core.domain.exception.entity_dynamic_mapping.ExpressionEvaluationFailedException",
        "com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateAlreadyExistsException",
        "com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateIdentifierCannotChangeException",
        "com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNameAlreadyExistsException",
        "com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateNotFoundException",
        "com.decathlon.idp_core.domain.exception.entity_template.EntityTemplateUsedByDynamicMappingException",
        "com.decathlon.idp_core.domain.exception.entity_template.PropertyDefinitionRulesConflictException",
        "com.decathlon.idp_core.domain.exception.entity_template.PropertyNameAlreadyExistsException",
        "com.decathlon.idp_core.domain.exception.entity_template.PropertyNameNotFoundEntityTemplatePropertiesException",
        "com.decathlon.idp_core.domain.exception.entity_template.PropertyTypeChangeException",
        "com.decathlon.idp_core.domain.exception.entity_template.RelationCannotTargetItselfException",
        "com.decathlon.idp_core.domain.exception.entity_template.RelationNameAlreadyExistsException",
        "com.decathlon.idp_core.domain.exception.entity_template.RelationNameNotFoundEntityTemplateRelationsException",
        "com.decathlon.idp_core.domain.exception.entity_template.RelationTargetTemplateChangeException",
        "com.decathlon.idp_core.domain.exception.entity_template.TargetTemplateNotFoundException",
        "com.decathlon.idp_core.domain.exception.filter.InvalidFilterDslException",
        "com.decathlon.idp_core.domain.exception.mock.MockSecurityConfigurationException",
        "com.decathlon.idp_core.domain.exception.search.InvalidSearchQueryException",
        "com.decathlon.idp_core.domain.exception.webhook.WebhookAuthenticationException",
        "com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorAlreadyExistException",
        "com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorConfigurationException",
        "com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorNotFoundException",
        "com.decathlon.idp_core.domain.exception.webhook.WebhookConnectorTitleAlreadyExistsException",
        "com.decathlon.idp_core.domain.exception.webhook.WebhookSecurityConfigurationException",};

    // Load the STATUS_BY_EXCEPTION map from ApiExceptionHandler
    var apiExceptionHandler = classLoader.loadClass(
        "com.decathlon.idp_core.infrastructure.adapters.api.handler.ApiExceptionHandler");
    var statusByExceptionField = apiExceptionHandler.getDeclaredField("STATUS_BY_EXCEPTION");
    statusByExceptionField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<Class<?>, ?> statusByException = (Map<Class<?>, ?>) statusByExceptionField.get(null);

    // Check all known exceptions are registered
    var unregistered = new java.util.ArrayList<String>();
    var missingExceptionClasses = new java.util.ArrayList<String>();
    for (String exceptionClassName : exceptionClassNames) {
      try {
        var exceptionClass = classLoader.loadClass(exceptionClassName);
        if (!statusByException.containsKey(exceptionClass)) {
          unregistered.add(exceptionClass.getSimpleName());
        }
      } catch (ClassNotFoundException ignored) {
        missingExceptionClasses.add(exceptionClassName);
      }
    }

    if (!missingExceptionClasses.isEmpty()) {
      throw new AssertionError(
          "The following exception classes listed in the architecture test cannot be loaded: "
              + missingExceptionClasses);
    }

    if (!unregistered.isEmpty()) {
      throw new AssertionError(
          "The following domain exceptions are not registered in ApiExceptionHandler.STATUS_BY_EXCEPTION: "
              + unregistered);
    }
  }
}
