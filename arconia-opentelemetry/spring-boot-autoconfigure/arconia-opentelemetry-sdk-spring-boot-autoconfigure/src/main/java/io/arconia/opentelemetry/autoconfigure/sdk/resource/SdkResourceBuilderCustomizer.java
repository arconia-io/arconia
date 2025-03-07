package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * Callback that can be used to customize the {@link ResourceBuilder}
 * used to build the auto-configured {@link io.opentelemetry.sdk.resources.Resource}.
 */
@FunctionalInterface
public interface SdkResourceBuilderCustomizer {

    void customize(ResourceBuilder builder);

}
