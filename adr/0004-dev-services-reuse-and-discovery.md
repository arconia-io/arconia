# 4. Dev Services reuse and discovery

Date: 2026-10-03

## Status

Accepted

## Context

The applications of a distributed system are often developed and run together on the same machine, and they interact through some of their services: they exchange messages through the same message broker, and they need their telemetry in the same observability platform to follow a request across them. If each application starts its own container for such a service, those interactions are lost. Sharing a container also saves resources for services that are heavy to run, such as a model inference server.

Testcontainers can reuse a container across runs, but it only reuses a container whose configuration hash matches exactly, so applications configured differently never share one. The feature also requires every developer to enable it in their own environment, and the Testcontainers project marks it as experimental.

The idea of sharing containers between applications is inspired by Quarkus Dev Services, which let applications find a container started by another one through container labels.

## Decision

Each Dev Service has a reuse strategy, configurable per Dev Service, among three. The `framework` strategy makes the container started by an application discoverable by the other applications using the same Dev Service, which connect to it instead of starting their own, whatever their configuration. The container is stopped with the application that started it. The `testcontainers` strategy reuses the container across runs through the Testcontainers feature, with its requirements: the developer enables the feature and removes the container when no longer needed, since Arconia never stops it. With the `none` strategy, every application starts its own container.

The defaults follow one rule: `framework` for the services applications interact through or that are heavy to run, such as message brokers and observability platforms, and `none` for data stores, whose state belongs to one application. Reuse only applies in dev mode, so that tests stay isolated. A configured strategy that cannot be applied falls back to `none`, and the startup message reports it with the reason.

An application connects to a discovered container with its own configuration, such as the credentials of its own Dev Service properties, which must match the ones of the application that started the container. When several containers are available, applications starting together converge on the same one. The labels identifying discoverable containers are a contract between applications, possibly running different Arconia versions, and do not change between versions.

## Consequences

Applications of the same system interact through shared brokers and observability platforms with no configuration, and the startup message states which container an application uses and why. A discovered container runs with the configuration of the application that started it, so configuration changes only take effect for that application. Discovery does not check whether the application that started a container is still alive: after a hard kill, the container runs for a few more seconds until the Testcontainers resource reaper removes it, and an application starting in that window can connect to it. This window is accepted. Making a Dev Service discoverable means providing the connection details for a discovered container, which must match the ones Spring Boot provides for a container the application starts itself.
