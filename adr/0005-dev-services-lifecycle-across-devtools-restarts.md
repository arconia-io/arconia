# 5. Dev Services lifecycle across DevTools restarts

Date: 2026-10-21

## Status

Accepted

## Context

During development, Spring Boot DevTools restarts the application every time the code or the configuration changes. If the containers of the Dev Services were restarted too, every change would wait for them to start again, and their data would be lost. Spring Boot solves this with `@RestartScope`, which keeps a container across DevTools restarts.

What Spring Boot doesn't provide is the rest of the lifecycle of such a container. Spring never cleans up what it keeps across restarts: the container keeps running when the application no longer needs it, and when the application stops, it runs on until Testcontainers notices that the process has ended and removes it, some seconds later. In the meantime, the container still holds the fixed ports it was given, and another application could discover it and connect to it right before it disappears.

## Decision

Arconia keeps the containers of the Dev Services across DevTools restarts, as Spring Boot does, and manages the lifecycle Spring Boot leaves out: a kept container is stopped as soon as it is no longer needed, and at the latest when the application stops.

When the application stops, it stops the containers it kept, so that no other application discovers a container about to disappear. After a restart, a kept container that is no longer needed is stopped right away: when the Dev Service backs off, for example because the application now defines its own `ConnectionDetails`, its container is stopped and any fixed port it holds is freed. A kept container that is no longer running, for example because it was removed by hand, is replaced with a new one. Otherwise, the application uses the container it kept, before looking for a container started by another application (see [4. Dev Services reuse and discovery](0004-dev-services-reuse-and-discovery.md)).

Two cases are left out on purpose. A container reused through the `testcontainers` strategy is never stopped by Arconia, since it is meant to outlive the application. A kept container is not replaced when the configuration of its Dev Service changes: the change takes effect once the application is stopped and started again, as with Spring Boot's `@RestartScope`, since the setup of a container also depends on customizer code and on the files copied into it, which cannot be compared reliably. Disabling a Dev Service through its `enabled` property also takes effect only on the next start: after a restart, a disabled Dev Service is skipped entirely, so its container keeps running until the application stops.

Containers are kept across restarts only when the application runs in dev mode with DevTools restarts enabled. In every other case, such as in tests or without DevTools, a container lives and stops with the application, as any Testcontainers container does in Spring Boot.

## Consequences

Restarts are fast and keep the data of the Dev Services, and containers don't outlive the application that started them. In exchange, changing the configuration of a Dev Service, including disabling it, requires stopping and starting the application, as the documentation states. An application that is killed rather than stopped cannot stop its containers, and Testcontainers removes them a few seconds later.
