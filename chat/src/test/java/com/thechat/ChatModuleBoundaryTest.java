package com.thechat;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Phase 4 exit criterion: Chat is a fully standalone service.
 *
 * This module's classpath now contains only:
 *   common  — shared kernel (DTOs, security primitives)
 *   chat's own bounded contexts: conversation, message, ws, realtime
 *   chat's own ACL package com.thechat.user (UserProfile, UserServiceClient — HTTP only)
 *
 * There is no :auth or :user Gradle module dependency (see chat/build.gradle) — this
 * test enforces that common, the one shared module, never grows a dependency back
 * into a bounded context, which would silently re-couple services at compile time.
 */
@AnalyzeClasses(packages = "com.thechat", importOptions = ImportOption.DoNotIncludeTests.class)
public class ChatModuleBoundaryTest {

    @ArchTest
    static final ArchRule commonMustNotDependOnBoundedContexts =
        noClasses()
            .that().resideInAPackage("com.thechat.common..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "com.thechat.conversation..",
                "com.thechat.message..",
                "com.thechat.ws..",
                "com.thechat.realtime..")
            .because("common is a shared kernel; importing bounded contexts breaks its contract.");
}
