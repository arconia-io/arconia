# 7. Semantic conventions

Date: 2025-09-09

## Status

Accepted

## Context

The Spring portfolio instruments applications through the Micrometer Observation API: Spring Framework for HTTP, Spring AI for model calls, and many libraries besides. The telemetry is named after Micrometer's own conventions, such as the `http.server.requests` metric. Observability backends increasingly expect other conventions: the OpenTelemetry Semantic Conventions, such as the `http.server.request.duration` metric, or the conventions of a specific platform for generative AI, such as OpenInference for Arize Phoenix or the ones of LangSmith and Langfuse. With telemetry named differently, dashboards, queries, and backend features built on those conventions don't work.

One option is to replace the Spring instrumentation with the instrumentation of the OpenTelemetry project. That loses or duplicates the observations the Spring projects maintain and keep in step with their own features, and it moves the application away from the Micrometer APIs that the Spring portfolio is built on. Another option is to rename the telemetry downstream, in the OpenTelemetry Collector or in the backend, which every deployment would have to configure and maintain.

## Decision

Arconia keeps the instrumentation of the Spring portfolio and changes how its telemetry is named and described, through the extension points of Micrometer, without changing the observations themselves. Only their names and attributes follow the chosen conventions.

The conventions are provided in modules, grouped by specification and by stability: adding the dependency is enough to adopt them, and each area can be turned off through its properties. The conventions defined as stable by their specification, such as the OpenTelemetry conventions for the JVM, HTTP, and errors, and the ones still in development, such as the conventions for generative AI, live in separate modules, so that the changes of a convention in development never affect the stable ones. Conventions that build on the same specification, such as the vendor conventions for generative AI built on the OpenTelemetry ones, are flavors of one module, selected through a property. Only one set of generative AI conventions can be active in an application, since two would describe the same observations differently.

The conventions only rely on the Micrometer APIs, so they work with any OpenTelemetry setup of the application: the Arconia OpenTelemetry Spring Boot Starter as well as the OpenTelemetry support of Spring Boot.

## Consequences

Applications get telemetry their backends understand by adding a dependency, with no change to their code or to their deployments. The telemetry is limited to what the Spring observations expose: when they lack the data a convention requires, the convention cannot be followed fully, and the gap is documented. Every convention set is a module Arconia maintains, following the evolution of its specification and of the Spring observations it applies to.
