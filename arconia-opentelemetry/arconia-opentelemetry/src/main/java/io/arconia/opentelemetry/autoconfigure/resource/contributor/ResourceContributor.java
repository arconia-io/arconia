package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * Contributes additional attributes to a {@link Resource}.
 */
@FunctionalInterface
public interface ResourceContributor {

    void contribute(ResourceBuilder builder);

}
