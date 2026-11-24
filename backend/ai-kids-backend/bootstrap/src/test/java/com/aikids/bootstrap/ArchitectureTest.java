package com.aikids.bootstrap;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.aikids", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("domain").definedBy("com.aikids.domain..")
            .layer("application").definedBy("com.aikids.application..")
            .layer("infrastructure").definedBy("com.aikids.infrastructure..")
            .layer("entrypoints").definedBy("com.aikids.entrypoints..")
            .layer("bootstrap").definedBy("com.aikids.bootstrap..")
            .whereLayer("bootstrap").mayNotBeAccessedByAnyLayer()
            .whereLayer("entrypoints").mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure").mayOnlyBeAccessedByLayers("bootstrap")
            .whereLayer("application").mayOnlyBeAccessedByLayers("infrastructure", "entrypoints", "bootstrap");

    @ArchTest
    static final ArchRule domainAndApplicationArePureJava = classes().that().resideInAnyPackage("com.aikids.domain..", "com.aikids.application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "com.aikids.domain..", "com.aikids.application..");

    @ArchTest
    static final ArchRule controllersUseOnlyInputPorts = noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("com.aikids.application.port.out..", "com.aikids.application.service..");
}
