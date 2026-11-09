# 2. Bootstrap mode profiles

Date: 2025-04-11

## Status

Accepted

## Context

The bootstrap mode (see [1. Bootstrap mode](0001-bootstrap-mode.md)) tells Arconia whether the application runs during development, in tests, or in production. The main use of that information is the one that motivated the mode: activating the profiles that hold the development and test settings, so that no launcher needs to be configured for them. Spring Boot users have asked for the same: a profile active only when running the application locally ([spring-boot#42066](https://github.com/spring-projects/spring-boot/issues/42066)), and configuration that applies to all tests without activating a profile in each of them ([spring-boot#38098](https://github.com/spring-projects/spring-boot/issues/38098)).

The idea is inspired by Quarkus, where each launch mode activates a profile of the same name, so that development and test settings apply without configuring any launcher.

Profiles are also the only way to activate configuration for a mode in Spring Boot. A configuration document can be activated for a profile or a cloud platform, and making the activation pluggable, which would allow activating it for a mode directly, is an open request ([spring-boot#23017](https://github.com/spring-projects/spring-boot/issues/23017)).

Spring Boot applications often activate profiles of their own in production, for the platform or the environment, and teams already have naming conventions for the development and test profiles of existing applications. Some applications don't want any profile to be activated for them.

## Decision

Arconia activates a profile in dev and test mode only: `dev` in dev mode and `test` in test mode, by default. No profile is activated in prod mode, so production keeps the profiles its deployment activates. 

The profiles of each mode can be customized as a list, with the `arconia.dev.profiles` and `arconia.test.profiles` properties, which replace the default. The mode profiles can be turned off entirely with the `arconia.bootstrap.profiles.enabled` property.

The mode profiles are activated before the profiles the user activates, so the profile-specific configuration of the user's profiles takes precedence over the one of the mode profiles. A mode profile that is already active is not activated again.

The profiles are activated before Spring Boot loads the application configuration, so that the profile-specific files are loaded by its regular import. For the same reason, these properties are only read from the sources available at that point: the `META-INF/arconia-bootstrap.properties` file, JVM system properties, environment variables, and command line arguments. Setting them in the application configuration has no effect and is reported with a warning.

## Consequences

Development and test settings go into `application-dev.yml` and `application-test.yml` and apply wherever the application or its tests run, with no launcher configuration. Teams with other profile names keep them by configuring the lists once, in the bootstrap properties file of the project, and applications that manage profiles themselves turn the feature off. Production profiles are never touched. Unlike the rest of the configuration, these properties cannot live in `application.yml`, which the warning makes visible.
