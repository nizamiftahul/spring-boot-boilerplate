package com.nizamiftahul.spring_boilerplate.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
		packages = "com.nizamiftahul.spring_boilerplate",
		importOptions = ImportOption.DoNotIncludeTests.class)
class ModularityRulesTest {

	@ArchTest
	static final ArchRule user_internal_packages_are_not_accessed_from_outside_the_module =
			classes()
					.that().resideInAnyPackage(
							"..modules.user.domain..",
							"..modules.user.application..",
							"..modules.user.web..")
					.should().onlyBeAccessed().byAnyPackage("..modules.user..");

	@ArchTest
	static final ArchRule sample_internal_packages_are_not_accessed_from_outside_the_module =
			classes()
					.that().resideInAnyPackage(
							"..modules.sample.domain..",
							"..modules.sample.application..",
							"..modules.sample.web..")
					.should().onlyBeAccessed().byAnyPackage("..modules.sample..");

	@ArchTest
	static final ArchRule modules_are_free_of_cycles =
			slices().matching("..modules.(*)..").should().beFreeOfCycles();

	@ArchTest
	static final ArchRule platform_does_not_depend_on_modules =
			noClasses()
					.that().resideInAPackage("..platform..")
					.should().dependOnClassesThat().resideInAnyPackage("..modules..");
}
