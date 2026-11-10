# 8. Semantic conventions conformance

Date: 2026-10-26

## Status

Accepted

## Context

The semantic conventions Arconia applies (see [7. Semantic conventions](0007-semantic-conventions.md)) are defined by specifications, but following a specification in practice involves choices it doesn't make for you. The reference instrumentations, such as the OpenTelemetry Java instrumentation, don't always match the text of the specification. Specifications in development change between releases, and their upcoming changes are visible well before they are released. The Spring observations don't always expose the data a convention requires, and the libraries Arconia builds on, such as Micrometer and Spring AI, have gaps of their own. The Spring observations also record attributes that the specifications don't define.

## Decision

Arconia follows the text of the specification. The reference instrumentations help understand it, but when they differ from it, the specification wins.

Arconia follows the latest released version of a specification. An unreleased change is adopted early only when it adds something, such as a new attribute or a recommended value, and never when it renames or removes something, since the release could still change it.

When a library Arconia builds on prevents following a convention, the fix belongs to that library. Arconia reports the problem upstream, documents the deviation with a link to the upstream issue, and doesn't change the internals of the library to work around it.

Attributes that the Spring observations record beyond the specification are kept, unless the convention defines an attribute that replaces them.

Defaults follow the convention too. Content that a specification makes opt-in, such as the prompts and completions of the OpenTelemetry conventions for generative AI, is not captured by default. Where a convention or the reference SDK of its platform captures content by default, as OpenInference and the vendor conventions do, Arconia does the same. Every convention can turn the capturing off.

Arconia supports the environment variables defined by a specification, such as the `OTEL_*` variables of OpenTelemetry and the `OPENINFERENCE_*` variables of OpenInference. It doesn't support the environment variables of vendor SDKs: the configuration properties cover the rest.

## Consequences

What Arconia emits can be checked against the text of a released specification, and each convention lists where and why it deviates. A deviation lasts until the library causing it is fixed, and in the meantime users can see it documented. Some attributes beyond the specification remain in the telemetry, as long as the specification doesn't replace them. Choosing a vendor flavor or OpenInference captures content by default, so applications sending personal or confidential data to their models must review it, or turn the capturing off, before exporting to a shared backend.
