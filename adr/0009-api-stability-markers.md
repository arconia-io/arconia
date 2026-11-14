# 9. API stability markers

Date: 2025-02-23

## Status

Accepted

## Context

A Java library makes a type or a method public for two different reasons: because applications are meant to use it, or because another package or module of the library itself needs it. Without the Java module system, which Spring Boot applications rarely use, the language cannot tell the two apart. Everything public looks like API, and whatever applications start using becomes hard to change.

Arconia is made of many modules that build on each other, so a good part of what is public is only there for other Arconia modules. Arconia also ships features early to gather feedback, such as the bootstrap mode, Dev Services, and multitenancy, whose API is expected to change as they mature. Users need to know which parts they can build on, which parts may still change, and which parts are not for them at all.

Three options were considered:

- A naming convention, with the non-API types placed in `internal` packages. It cannot mark a single method of an otherwise public type, and changing the status of a type means moving it to another package, which breaks the code using it.
- The API Guardian `@API` annotation, used by JUnit. It would add a dependency to every Arconia module and to the applications using them, and its five statuses are more than Arconia needs.
- Arconia's own annotations.

## Decision

Arconia marks the stability of its public types and methods with two annotations of its own, `@Incubating` and `@Internal`, provided by a module with no dependencies that every other module can use.

`@Incubating` marks API that applications can use, but that may change or be removed in any release, without notice or a deprecation period. A feature is incubating until its API has proven itself in use. Removing the marker is a deliberate decision made for each API, never a side effect of a release.

`@Internal` marks types and methods that are public only because other parts of Arconia need them. They are not API: applications must not use them, and they may change or disappear in any release.

Everything public that carries neither marker is API meant for applications. A marker on a type applies to all its members, and a marker on a method applies to that method alone, so a public type can expose a single internal method without becoming internal itself.

Both markers are part of the generated Javadoc, so the status of a type or method is visible where it is documented and in the IDE.

## Consequences

Users can tell from the Javadoc or the source what they can rely on, and Arconia can share code between its modules and release features early without committing to their API.

The markers inform but don't enforce: nothing stops an application from using an internal type, and nothing fails the build when an incubating API changes. Test-only hooks don't get an internal marker to justify being public. The behavior is made testable through the regular API instead.

This decision only defines the markers. The compatibility that Arconia guarantees for the API carrying neither marker, including how changes to it are announced, is not part of it and will be defined with the first GA release.
