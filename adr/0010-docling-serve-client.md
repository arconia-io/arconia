# 10. Docling Serve client

Date: 2026-02-04

## Status

Accepted

## Context

Docling Java defines the `DoclingServeApi` interface for calling Docling Serve, together with the request and response models, and provides a client implementing it. Any library can provide its own implementation, and `DoclingServeApi.builder()` builds whichever one is available on the classpath.

The Docling Java client manages its own HTTP stack on top of the JDK `HttpClient`, with its own JSON mapping, logging, and timeouts. In a Spring Boot application, that means the calls to Docling Serve aren't observed, and they don't use the SSL bundles, the HTTP client settings, or the `RestClient` customizers that apply to every other HTTP client of the application.

The async operations of the interface return a `CompletionStage`. Docling Serve runs them as tasks: the client submits the task, polls its status until it completes, and then fetches its result.

## Decision

Arconia implements `DoclingServeApi` on Spring's `RestClient`, starting from the `RestClient.Builder` configured by Spring Boot. The client gets the Micrometer observations, the message converters, the customizers, and the HTTP client settings of Spring Boot, and its connection is configured with configuration properties and connection details, like any Spring Boot integration. Applications code against the interface and the models of Docling Java, so code written for Docling Java keeps working.

Arconia registers its client as the Docling Java implementation, so `DoclingServeApi.builder()` builds it too. A client built that way only gets the options of Docling Java, not the Spring Boot configuration.

Each operation is written with the `RestClient` API rather than declared in an HTTP interface. HTTP interfaces backed by `RestClient` only support blocking return types, and Spring Framework declined to support `CompletableFuture` there in favor of structured concurrency ([spring-framework#34748](https://github.com/spring-projects/spring-framework/issues/34748)). Even with that support, an async operation is a sequence of calls rather than a single request. The async operations run on virtual threads by default and block while they poll.

## Consequences

Calls to Docling Serve are observed, secured, and configured like the other HTTP clients of a Spring Boot application, and the Arconia features built on the client, such as the health indicator, Dev Services, and the document reader, share the same configuration.

Arconia follows Docling Java: every new operation or option of the interface has to be implemented in Arconia, and the parts of the API that come from Docling Java change when Docling Java changes them, independently of the stability of the Arconia API.

Only one implementation can be on the classpath for `DoclingServeApi.builder()` to work. An application that also has the Docling Java client gets an error from `DoclingServeApi.builder()`, while the auto-configured client keeps working.
