# 1. Bootstrap mode

Date: 2025-04-11

## Status

Accepted

## Context

Applications usually need different settings during development and in tests, such as a local database, more detailed logging, or test-only beans. Spring Boot supports that with profiles, but has no concept of a dev or test mode that would activate them, so the right profile must be activated in every place the application is launched from:

- the IntelliJ IDEA run configuration, which has its own setting for the active profiles and doesn't use the ones configured in the build;
- the Gradle build, once for the `bootRun` task and once for the `test` task, or the Maven build, in the Spring Boot Maven Plugin and in the Surefire Plugin;
- the VS Code launch configuration, and its separate configuration for running tests;
- the tests themselves, for example with `@ActiveProfiles("test")` on each test class.

Each of these places is easy to forget, differs between team members, and has to be repeated in every project. A launcher that is missed runs with the wrong settings. A bootstrap mode has been proposed for Spring Boot itself ([spring-boot#46368](https://github.com/spring-projects/spring-boot/issues/46368)), where the Spring Boot team noted that detecting launches from an IDE consistently is the main difficulty.

Spring Boot's own development-time features each decide on their own instead. Spring Boot DevTools enables itself with heuristics: it stays disabled in a native image and when the call stack contains a test framework or the AOT processor, and its restart support only starts from the main thread when the application class loader is the one Java uses at development time. DevTools also applies development defaults for properties, which any module can contribute since Spring Boot 4.0 ([spring-boot#44792](https://github.com/spring-projects/spring-boot/issues/44792)), so development-only settings depend on DevTools being on the class path. The Docker Compose support starts its services whenever the module is on the class path and skips them in tests by default. Testcontainers at development time needs a separate main class on the test class path, launched by a dedicated task.

Arconia needs a single answer for all its features that behave differently during development, in tests, and in production: the profiles activated for each context, the Dev Services, and the beans registered only during development. The answer must be available before the application configuration is loaded, to activate the profiles, and must remove the per-launcher configuration rather than move it elsewhere.

Three options were considered:

- Setting the mode at build time, through dedicated Arconia Gradle and Maven plugins configuring the run and test tasks. Quarkus works this way: its launch mode is set by the tooling that starts the application, such as its dev mode command, its test extension, or the packaged application. For Spring Boot applications, it would not cover running the application or the tests from an IDE, which launches the main class or the test framework directly, unless every IDE configuration is set up as well.
- Leaving it to the user to set the mode in every launcher. That is the same configuration burden profiles have, which the mode is meant to remove.
- Detecting the mode at runtime from how the application is launched, as DevTools does.

## Decision

The bootstrap mode is detected at runtime, building on the DevTools heuristics. When it is not set explicitly, the first matching rule decides:

1. `prod` when Spring AOT processes the application, so that the production behavior is baked into the AOT artifacts.
2. `test` when the call stack contains a test framework: the DevTools list (JUnit, Spring Boot Test, Cucumber), extended with TestNG and the current Cucumber packages.
3. `prod` in a GraalVM native image.
4. `dev` when DevTools is on the class path, or when the application class loader is the one Java uses at development time and the class path contains the output directory of a build tool or IDE (Gradle, Maven, IntelliJ IDEA, Eclipse).
5. `prod` otherwise.

The output directories are matched by their conventional paths rather than by any directory on the class path, as DevTools does. DevTools never reaches production, since the build plugins leave it out of the packaged application, so it can afford a loose rule. The bootstrap mode is detected in the packaged application too, where a loose rule would detect an exploded class path, as in container images, as `dev`. A wrong `prod` during development is visible and fixed by setting the mode explicitly; a wrong `dev` in production is silent. The rule favors the former.

The mode can be set explicitly with the `arconia.bootstrap.mode` JVM system property or the `ARCONIA_BOOTSTRAP_MODE` environment variable, the JVM system property taking precedence. It is never read from the application configuration: the mode describes how the application is launched, the configuration is packaged with the application and ships to production, and the mode is needed before the configuration is loaded to activate the mode profiles. An invalid explicit value falls back to `prod` with a warning. The mode is detected once and cached for the lifetime of the JVM, so that it stays the same across DevTools restarts.

## Consequences

Running the application from an IDE or a build tool, running the tests, and running the packaged application work with no configuration. The Arconia CLI sets the mode explicitly for the processes it launches. The mode alone decides which profiles are activated and which beans are registered only for a given mode, including in the packaged application, so the detection errs toward `prod`: a launch the heuristics don't recognize runs in `prod` mode and can set the mode explicitly.
