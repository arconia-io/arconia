# 15. OpenTelemetry and Spring Boot

Date: 2026-11-19

## Status

Accepted

## Context

Spring Boot builds observability on Micrometer, and supports OpenTelemetry on top of it. It bridges Micrometer Tracing to the OpenTelemetry SDK, configures the SDK for traces and logs, exports both via OTLP, and since version 4.1 maps the OpenTelemetry environment variables to its own properties. Metrics are different: Spring Boot exports the Micrometer metrics via OTLP with the Micrometer registry and doesn't support the OpenTelemetry Metrics API, so the metrics recorded through that API by a library are never exported.

The configuration follows the same split. Traces and logs are configured under `management.opentelemetry.*` and metrics under `management.otlp.metrics.export.*`, each with its own endpoint, timeout, transport, and certificates. Spring Boot 4.2 lets the three signals share the endpoint, the headers, and the compression ([spring-boot#50461](https://github.com/spring-projects/spring-boot/pull/50461)), and nothing else. A configuration model for OpenTelemetry as a whole is still an open request ([spring-boot#47962](https://github.com/spring-projects/spring-boot/issues/47962)), as is bridging the logging frameworks to OpenTelemetry ([spring-boot#47995](https://github.com/spring-projects/spring-boot/issues/47995)).

Arconia OpenTelemetry exists to give a Spring Boot application the three signals, both instrumentation APIs, and one configuration model with a single dependency. One way of getting there is to build on the OpenTelemetry support of Spring Boot and add what's missing. As long as Spring Boot has no configuration model shared by the three signals, that would configure the same exporter in two places: the settings Spring Boot knows under `management.*` and the rest under `arconia.otel.*`. The other way is for Arconia to configure OpenTelemetry itself. That is what it has done so far, but with no stated rule for which Spring Boot properties still apply: some were followed and others silently ignored.

## Decision

Arconia configures the OpenTelemetry SDK and the exporters of the three signals itself, under `arconia.otel.*` and through the environment variables of the OpenTelemetry specification. It doesn't use the OpenTelemetry auto-configuration of Spring Boot, and combining the Arconia OpenTelemetry Spring Boot Starter with the Spring Boot one is not supported.

The namespace of a Spring Boot property tells whether it applies. The properties under `management.opentelemetry.*` and `management.otlp.*` are replaced by the Arconia ones and have no effect. When an application sets one of them, Arconia logs a warning at startup that names the property to use instead. All the other `management.*` properties apply as they do in any Spring Boot application, because they belong to Micrometer and to Spring Boot rather than to OpenTelemetry: the sampling probability, the propagation formats, the baggage, the switches that turn the export of a signal on and off, and the configuration of metrics and observations. Arconia defines no property of its own for what they cover.

Everything below the OpenTelemetry configuration stays the one of Spring Boot: the Micrometer instrumentation and its auto-configuration, the SSL bundles, the service connections, and the support for virtual threads.

Arconia holds this part of the configuration only until Spring Boot can. What Spring Boot is missing is proposed upstream first, and a new feature in Arconia is designed the way it would fit Spring Boot, so that it can be contributed. A new Arconia property for something Spring Boot already has a property for takes its name and values from there. Customizations are made by defining beans of the OpenTelemetry SDK and Micrometer types, which work the same way in both.

The hand-over happens in two steps. Once the common OTLP configuration of Spring Boot covers every setting of the exporters, Arconia adopts the Spring Boot configuration for traces and logs, and its metrics exporter reads the same common properties. Once Spring Boot supports the OpenTelemetry Metrics API, Arconia keeps only the features Spring Boot doesn't have.

## Consequences

An application configures OpenTelemetry in one place, with the same settings for the three signals, and the environment variables of the specification work as they do in any other OpenTelemetry SDK. An application moving from the OpenTelemetry support of Spring Boot renames its properties, which the migration recipe does, and a property left behind is reported at startup instead of being ignored.

The Spring Boot features that rely on the `management.*` properties keep working, such as turning off the export of traces and metrics in tests.

Arconia has to keep up with Spring Boot. Whatever a new Spring Boot version adds to its OpenTelemetry configuration reaches the applications using Arconia only after Arconia adds it too, so each Spring Boot release is compared with Arconia.

Each step of the hand-over renames properties and is a breaking change, shipped with a migration recipe.
