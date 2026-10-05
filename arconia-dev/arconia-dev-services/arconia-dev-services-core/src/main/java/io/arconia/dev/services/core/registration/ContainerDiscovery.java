package io.arconia.dev.services.core.registration;

import java.util.List;

/**
 * Discovers the running containers of a dev service that were started by other
 * applications and made discoverable.
 */
interface ContainerDiscovery {

    /**
     * Discover the containers providing the dev service with the given name.
     *
     * @param serviceName the name of the dev service to look for
     * @return the discovered containers, oldest first, or an empty list when
     * none is available or the container runtime cannot be queried
     */
    List<DiscoveredContainer> discover(String serviceName);

}
