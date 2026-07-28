package io.arconia.dev.services.api.config;

import io.arconia.core.support.Incubating;

/**
 * Properties for dev services that support sharing among applications
 * running simultaneously.
 */
@Incubating
public interface SharedDevServicesProperties extends BaseDevServicesProperties {

    /**
     * Whether the dev service is shared among applications running simultaneously.
     * A shared dev service is discoverable by other applications, and the application
     * connects to an existing shared dev service if available instead of starting a new one.
     * Sharing is independent of container reuse: a reused container may also be shared.
     * Only applicable in dev mode.
     */
    default boolean isShared() {
        return false;
    }

}
