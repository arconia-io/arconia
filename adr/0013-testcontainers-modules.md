# 13. Testcontainers modules

Date: 2026-01-10

## Status

Accepted

## Context

Dev Services and Spring Boot service connections run their services with Testcontainers, which needs a container type for each service: one that knows its image, its ports, when it's ready, and how to configure it for development and testing. The Testcontainers project provides modules for many services, and the community provides more, often maintained by the vendor of the service.

Some services have no module at all, such as OpenLIT, Arize Phoenix, and Langfuse. Others have one that isn't maintained reliably. The Redis module from Redis Inc. went from February 2025 to October 2026 without a release, and the version Spring Boot manages is still the last one before that silence. A module that falls behind the releases of Testcontainers or of the service blocks every upgrade of the projects depending on it.

Configuring a generic container inside each Dev Service would avoid a separate module, but it would tie the setup of the service to Spring Boot and to Dev Services, and nobody could use it in plain Testcontainers code.

## Decision

Arconia provides a Testcontainers module for a service only when no actively maintained module exists for it, from the Testcontainers project or from the community. A module is actively maintained when its releases keep up with the releases of Testcontainers and of the service. Otherwise, Arconia uses the existing module, as the Dev Services do for most services.

A module that resumes its releases after a long silence doesn't replace the Arconia one right away. Arconia keeps its own module until the other has shown sustained maintenance again, which is the case of Redis at the time of writing.

Each Arconia module covers one service and works in any Java project using Testcontainers, with no dependency on Spring. The Spring Boot service connections for its container live in the Arconia module of the technology using the service, such as Arconia OpenTelemetry for the observability platforms. The container sets the defaults that suit development and testing, such as turning off authentication and telemetry or creating an initial account, and the user overrides them with environment variables.

## Consequences

The services Arconia supports are available to plain Testcontainers code, to Spring Boot service connections, and to Dev Services in the same way, with the same defaults. Arconia maintains each module: it follows the changes of the image, such as a new readiness endpoint or a new way of receiving data, and tests the module against a pinned version of the image.

An Arconia module and a community module can exist for the same service, as for Redis, where Spring Boot connects applications to either. Replacing an Arconia module with a community one later on removes the Arconia module, which is a breaking change for the projects using it directly.
