package com.decathlon.idp_core.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/// Architecture guardrails enforcing the "Pragmatic Hexagonal Architecture" decided in
/// ADR-0002 and detailed in the `.github/instructions` layer guidelines.
///
/// Some rules enforced are:
/// - The domain stays independent from the infrastructure and from third-party integration libraries
/// - Ports are pure contracts
@AnalyzeClasses(packages = HexagonalArchitectureTest.BASE_PACKAGE, importOptions = DoNotIncludeTests.class)
class HexagonalArchitectureTest {

  static final String BASE_PACKAGE = "com.decathlon.idp_core";

  private static final String DOMAIN_PACKAGE = "..domain..";
  private static final String INFRASTRUCTURE_PACKAGE = "..infrastructure..";
  private static final String PORT_PACKAGE = "..domain.port..";
  private static final String ADAPTERS_PACKAGE = "..infrastructure.adapters..";
  private static final String API_CONTROLLER_PACKAGE = "..adapters.api.controller..";
  private static final String API_DTO_PACKAGE = "..adapters.api.dto..";
  private static final String PERSISTENCE_PACKAGE = "..adapters.persistence..";

  private static final String SPRING_TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
  private static final String JAKARTA_TRANSACTIONAL = "jakarta.transaction.Transactional";

  /// Third-party / framework integration packages the domain must never depend
  /// on.
  ///
  /// The list is intentionally explicit so the allowed Spring
  /// annotations (e.g. `@Service`, `@Component`) and Jakarta Validation
  /// (`jakarta.validation..`) are not caught as false positives.
  private static final String[] FORBIDDEN_TECHNOLOGY_PACKAGES = {"jakarta.persistence..", // JPA
      "org.springframework.web..", "org.springframework.data.jpa..",
      "org.springframework.data.repository..", "org.springframework.http..", "org.hibernate..",
      "org.mapstruct..", "com.schibsted..", "com.fasterxml.jackson..", "org.apache.kafka..",
      "java.net.http..",};

  // ---------------------------------------------------------------------------------------
  // 1. Hexagonal constraints
  // ---------------------------------------------------------------------------------------

  /// The domain must never depend on the infrastructure
  @ArchTest
  static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses().that()
      .resideInAPackage(DOMAIN_PACKAGE).should().dependOnClassesThat()
      .resideInAPackage(INFRASTRUCTURE_PACKAGE)
      .because("the domain is the inside the hexagon and must not depend on adapters");

  /// The domain is pure business logic and must not depend on integration
  /// frameworks
  /// Jakarta Validation is allowed
  @ArchTest
  static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_FORBIDDEN_TECHNOLOGY = noClasses().that()
      .resideInAPackage(DOMAIN_PACKAGE).should().dependOnClassesThat()
      .resideInAnyPackage(FORBIDDEN_TECHNOLOGY_PACKAGES)
      .because("the domain must stay free of integration frameworks");

  /// Ports are contracts: every class in the port package must be an interface
  @ArchTest
  static final ArchRule PORTS_MUST_BE_INTERFACES = classes().that().resideInAPackage(PORT_PACKAGE)
      .should().beInterfaces()
      .because("ports define the contract between domain and adapters and carry no implementation");

  /// Every `*Port` contract must live in the dedicated port package
  @ArchTest
  static final ArchRule PORT_NAMED_CLASSES_MUST_LIVE_IN_PORT_PACKAGE = classes().that()
      .haveSimpleNameEndingWith("Port").should().resideInAPackage(PORT_PACKAGE)
      .because("port contracts must be grouped under domain.port");

  /// A driven port may only be implemented by an infrastructure adapter, no other
  /// layer is allowed to provide a port implementation
  @ArchTest
  static final ArchRule PORTS_IMPLEMENTED_ONLY_IN_ADAPTERS = classes().that()
      .implement(resideInAPackage(PORT_PACKAGE)).should().resideInAPackage(ADAPTERS_PACKAGE)
      .because("ports are provided by domain and implemented by infrastructure adapters only");

  /// The domain must not depend on JPA persistence entities
  @ArchTest
  static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_JPA_ENTITIES = noClasses().that()
      .resideInAPackage(DOMAIN_PACKAGE).should().dependOnClassesThat()
      .haveSimpleNameEndingWith("JpaEntity")
      .because("persistence entities are an infrastructure detail, the domain owns its own model");

  /// The infrastructure may depend on the domain, but the domain must never reach
  /// into the infrastructure
  @ArchTest
  static final ArchRule LAYER_DEPENDENCIES_POINT_INWARD = layeredArchitecture()
      .consideringOnlyDependenciesInLayers().layer("Domain").definedBy(DOMAIN_PACKAGE)
      .layer("Infrastructure").definedBy(INFRASTRUCTURE_PACKAGE).whereLayer("Infrastructure")
      .mayNotBeAccessedByAnyLayer().because(
          "dependencies point inward: infrastructure depends on the domain, never the reverse");

  /// Request/response DTOs are a driving-adapter and must live with the REST API
  @ArchTest
  static final ArchRule DTOS_RESIDE_IN_API_DTO_PACKAGE = classes().that()
      .haveSimpleNameEndingWith("Dto").or().haveSimpleNameEndingWith("DtoIn").or()
      .haveSimpleNameEndingWith("DtoOut").should().resideInAPackage(API_DTO_PACKAGE)
      .because("DTOs are an API-adapter concern and must stay under adapters.api.dto");

  /// Controllers must go through domain ports and never reach directly the
  /// persistence adapter
  @ArchTest
  static final ArchRule CONTROLLERS_MUST_NOT_ACCESS_PERSISTENCE = noClasses().that()
      .resideInAPackage(API_CONTROLLER_PACKAGE).should().dependOnClassesThat()
      .resideInAPackage(PERSISTENCE_PACKAGE)
      .because("controllers must call the domain through ports, never the persistence adapter");

  /// Transaction boundaries belong to the domain service layer, controllers
  /// should not
  /// be annotated with `@Transactional` at class level
  @ArchTest
  static final ArchRule TRANSACTIONAL_NOT_ON_CONTROLLERS = noClasses().that()
      .resideInAPackage(API_CONTROLLER_PACKAGE).should().beAnnotatedWith(SPRING_TRANSACTIONAL)
      .orShould().beAnnotatedWith(JAKARTA_TRANSACTIONAL)
      .because("transaction boundaries belong in the domain service layer, not the web layer");

  /// Transaction boundaries belong to the domain service layer; controller
  /// methods should not be annotated with `@Transactional`
  @ArchTest
  static final ArchRule TRANSACTIONAL_METHODS_NOT_ON_CONTROLLERS = noMethods().that()
      .areDeclaredInClassesThat().resideInAPackage(API_CONTROLLER_PACKAGE).should()
      .beAnnotatedWith(SPRING_TRANSACTIONAL).orShould().beAnnotatedWith(JAKARTA_TRANSACTIONAL)
      .because("transaction boundaries belong in the domain service layer, not the web layer");
}
