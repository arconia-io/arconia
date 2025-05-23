package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * Callback that can be used to customize the {@link ResourceBuilder}
 * used to build the auto-configured {@link io.opentelemetry.sdk.resources.Resource}.
 *
 * @deprecated in favour of {@link OpenTelemetryResourceBuilderCustomizer}.
 */
@FunctionalInterface
@Deprecated(since = "0.12.0", forRemoval = true)
public interface SdkResourceBuilderCustomizer {

    void customize(ResourceBuilder builder);

}
