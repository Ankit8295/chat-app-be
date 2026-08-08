package com.thechat;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Auth service boundary rule: Phase 2 exit criterion.
 * Auth must NOT depend on com.thechat.user.* or com.thechat.chat.*.
 * Breaking this rule means auth re-introduces a coupling that Phase 2 removed.
 */
@AnalyzeClasses(packages = "com.thechat", importOptions = ImportOption.DoNotIncludeTests.class)
public class AuthModuleBoundaryTest {

    @ArchTest
    static final ArchRule authMustNotDependOnUser =
        noClasses()
            .that().resideInAnyPackage("com.thechat.auth..", "com.thechat.security..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("com.thechat.user..", "com.thechat.friendship..")
            .because("Phase 2 exit: auth must own credentials independently of the User service.");

    @ArchTest
    static final ArchRule authMustNotDependOnChat =
        noClasses()
            .that().resideInAnyPackage("com.thechat.auth..", "com.thechat.security..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.thechat.conversation..",
                "com.thechat.message..",
                "com.thechat.ws..",
                "com.thechat.realtime..")
            .because("Auth must never depend on Chat.");
}
