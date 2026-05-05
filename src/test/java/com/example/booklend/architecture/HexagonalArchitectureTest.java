package com.example.booklend.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.example.booklend", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
                .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.transaction.."
                )
            .because("domain layer must be framework-free and independently compilable");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("domain is the core — it must not depend on infrastructure adapters");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_application =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .because("domain is the innermost layer — it has no outward dependencies");

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("application layer defines ports — it must not depend on adapter implementations");

    @ArchTest
    static final ArchRule inbound_adapters_must_not_access_services_directly =
        noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..application..service..")
            .because("inbound adapters must call use case ports, not service implementations — " +
                     "the port is the contract, the service is a swappable detail");

    @ArchTest
    static final ArchRule inbound_adapters_must_not_access_persistence_directly =
        noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..persistence..")
            .because("inbound adapters must not bypass the port and reach into persistence");

    @ArchTest
    static final ArchRule bounded_contexts_must_be_free_of_cycles =
        slices().matching("com.example.booklend.(catalog|lending|member|shared)..")
            .should().beFreeOfCycles()
            .because("bounded contexts must not have circular dependencies");
}
