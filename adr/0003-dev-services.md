# 3. Dev Services

Date: 2025-04-11

## Status

Accepted

## Context

Most applications depend on external services, such as a database, a message broker, or an observability platform, and need them while developing and testing. Spring Boot supports running them as containers at development time with Testcontainers ([Spring Boot development-time services](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.testcontainers)), but every application has to wire them itself: declare the Testcontainers modules, define the containers as beans in a test configuration class annotated for the service connection and the DevTools restart scope, write a test main class that adds that configuration, run the application from the test class path with a dedicated task, and import the configuration in every test class needing the services. The same code is written in every project and configured in Java rather than in properties. Running the application changes too: it needs another command, which can only be found by inspecting the project, and the second main class on the test class path confuses IDEs, which may pick the wrong one. These challenges, and a zero-code evolution of the feature, have been proposed to Spring Boot ([spring-boot#46367](https://github.com/spring-projects/spring-boot/issues/46367)). Spring Boot's Docker Compose support is the alternative, at the cost of maintaining a Compose file and running without it in tests.

The idea is inspired by Quarkus Dev Services: when an application includes an extension and doesn't configure the connection to its service, Quarkus starts the service in a container in dev and test mode and connects the application to it.

Spring Boot already has a strategy for removing this kind of repeated setup: starter dependencies and auto-configuration. Adding a dependency brings a capability with sensible defaults, configured through properties, which backs off when the application defines its own.

## Decision

Arconia applies the Spring Boot strategy of starter dependencies and auto-configuration to Spring Boot's Testcontainers support. A Dev Service is a module for one service: when it is on the class path and the application runs in dev or test mode (see [1. Bootstrap mode](0001-bootstrap-mode.md)), it starts a container for the service and connects the application to it. One dependency per service, and no code.

Dev Services build on Spring Boot's Testcontainers support instead of replacing it. Containers are registered as beans with a service connection, so that Spring Boot provides the connection details to the application as for a container the application defines itself. Testcontainers provides the containers.

The connection of a Dev Service takes precedence over the one the application configures through properties, as for any service connection in Spring Boot, so that development and tests never connect to the services configured for production. Where Spring Boot has no connection details for a service, the Dev Service sets the connection properties itself, with the same precedence.

Each Dev Service is configured through properties under `arconia.dev.services.<service-name>`, sharing a common set (enabled, image, port, environment, startup timeout, reuse strategy, and more) next to the properties specific to the service, with defaults that work out of the box. Container customizer beans cover what properties don't. A Dev Service backs off when it is disabled, or when the application defines its own connection details for the service, so that it coexists with Spring Boot's Testcontainers and Docker Compose support.

The application runs with its usual commands and its tests need no imports: the Dev Services are active wherever the application or its tests run in dev or test mode. Custom Dev Services are built with the same building blocks as the built-in ones.

Like the DevTools and Docker Compose support of Spring Boot, Dev Services are development and test dependencies: `testAndDevelopmentOnly` with Gradle and `optional` with Maven, which the Spring Boot build plugins leave out of the packaged application (for Maven, by default since Spring Boot 4.0, [spring-boot#25403](https://github.com/spring-projects/spring-boot/issues/25403)). Dependency scoping is what keeps them out of production. The bootstrap mode only decides when they are active, and no other guard is added. The modules are not marked as development tools in their manifest ([27715de](https://github.com/spring-projects/spring-boot/commit/27715dee202af68dbb0d559196ab4a438d90aafb)), since the build plugins would also leave them out of the `bootRun` and `spring-boot:run` class paths, where they are needed.

## Consequences

Adding a dependency is enough to develop and test against a service, and the settings live in the application configuration. Dev Services are opinionated: the properties cover the common scenarios, and applications needing full control over a container keep Spring Boot's Testcontainers support for that service, side by side with the Dev Services for the others. Every supported service is a module Arconia maintains, following the evolution of its image and of its Testcontainers module. Dev Services need a container runtime during development and tests. Dev Services are not available in AOT-processed applications and tests, since whether a container is started or discovered is decided at startup and cannot be generated ahead of time, the same limitation Spring Boot has for service connections defined as beans. A Dev Service can only reach production through a dependency declared with a broader scope than documented, which is a build configuration error rather than a runtime one.
