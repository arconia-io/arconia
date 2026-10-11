package io.arconia.dev.services.api.config;

import io.arconia.core.support.Incubating;

/**
 * Strategies for reusing a running container for a dev service, across applications
 * or restarts, instead of starting a new one.
 */
@Incubating
public enum ReuseStrategy {

    /**
     * A new container is always started for the dev service
     * and stopped together with the application.
     */
    NONE,

    /**
     * The dev service is discoverable by other applications running simultaneously,
     * and the application connects to an existing dev service if available instead
     * of starting a new one. The container is stopped together with the application
     * that started it.
     */
    FRAMEWORK,

    /**
     * The container is kept running across application runs, relying on the Testcontainers
     * reusable containers feature. It is up to you to enable the feature, via
     * {@code testcontainers.reuse.enable=true} in the {@code ~/.testcontainers.properties}
     * file or the {@code TESTCONTAINERS_REUSE_ENABLE=true} environment variable, and to
     * clean up afterwards: such containers are never stopped automatically.
     */
    TESTCONTAINERS

}
