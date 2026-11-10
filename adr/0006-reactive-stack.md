# 6. Reactive stack

Date: 2026-08-24

## Status

Accepted

## Context

Spring offers two stacks for building applications. The servlet stack is built on Spring MVC, blocking HTTP clients such as `RestClient`, and JDBC. The reactive stack is built on Spring WebFlux, `WebClient`, R2DBC, and Project Reactor. Most of the Spring community builds applications on the servlet stack.

Supporting both stacks means building every feature twice. Instrumentation conventions, connections to services, security integrations, and the resolution of the current tenant all work differently on the reactive stack: the request is represented differently, and the context of an operation is propagated differently. The reactive programming model is also harder to reason about, which makes these features harder to get right, to test, and to troubleshoot. The cost would apply to every feature, for the benefit of a minority of applications.

## Decision

Arconia targets the servlet stack. Its features are built and tested for Spring MVC, the blocking HTTP clients, and JDBC, and Arconia doesn't provide reactive variants of them for Spring WebFlux, `WebClient`, or R2DBC.

Reactive applications keep the behavior of Spring Boot where Arconia doesn't extend it. For example, the HTTP server and client observations of a reactive application keep the conventions of Spring Framework, since the OpenTelemetry conventions only cover the servlet stack and the blocking clients.

## Consequences

Each feature has a single implementation, which keeps Arconia simpler to build, test, and maintain. Reactive applications can still use Arconia, but get fewer of its features, and the documentation of a feature states when it only covers the servlet stack. Requests for reactive variants of a feature are declined.
