package io.arconia.opentelemetry.autoconfigure.resource;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * Callback that can be used to customize the {@link ResourceBuilder}
 * used to build the auto-configured {@link Resource}.
 */
@FunctionalInterface
public interface SdkResourceBuilderCustomizer {

    void customize(ResourceBuilder builder);

}
