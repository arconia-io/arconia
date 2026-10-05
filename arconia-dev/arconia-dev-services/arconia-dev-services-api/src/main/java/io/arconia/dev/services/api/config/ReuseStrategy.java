package io.arconia.dev.services.api.config;

import io.arconia.core.support.Incubating;

/**
 * Strategies for reusing a running container for a dev service
 * across applications instead of starting a new one.
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
     * The container is kept running across application restarts, relying on the
     * Testcontainers reusable containers feature, which must be enabled in the
     * {@code ~/.testcontainers.properties} file. Such containers are not stopped
     * automatically and must be cleaned up manually.
     */
    TESTCONTAINERS

}
