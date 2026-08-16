package io.arconia.dev.services.core.registration;

import org.jspecify.annotations.Nullable;

/**
 * Looks up a running container that provides a shared dev service, started by
 * another application.
 * <p>
 * Implementations never return a container started by the current application:
 * adopting our own container would make the application connect to a dev service
 * it is also responsible for starting.
 */
interface SharedContainerLocator {

    /**
     * Find a container providing the shared dev service with the given name.
     *
     * @param serviceName the name of the dev service to look for
     * @return the container to adopt, or {@code null} when none is available or
     * the container runtime cannot be queried
     */
    @Nullable
    DiscoveredContainer locate(String serviceName);

}
