package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * Callback for customizing the {@link ResourceBuilder}
 * used to build the auto-configured {@link Resource}.
 */
@FunctionalInterface
public interface OpenTelemetryResourceBuilderCustomizer {

    void customize(ResourceBuilder builder);

}
