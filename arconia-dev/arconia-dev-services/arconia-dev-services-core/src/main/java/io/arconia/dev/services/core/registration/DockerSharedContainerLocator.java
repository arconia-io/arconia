package io.arconia.dev.services.core.registration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import io.arconia.dev.services.core.container.DevServiceLabels;

/**
 * Locates shared dev service containers by querying the OCI runtime for the labels
 * Arconia applies when starting a shared container.
 * <p>
 * The lookup is label-based and config-agnostic: the container is adopted as it runs,
 * with the owning application's configuration.
 */
final class DockerSharedContainerLocator implements SharedContainerLocator {

    private static final Logger logger = LoggerFactory.getLogger(DockerSharedContainerLocator.class);

    private static final String STATUS_RUNNING = "running";

    @Override
    @Nullable
    public DiscoveredContainer locate(String serviceName) {
        try {
            // Get Docker client from Testcontainers. We don't close the connection as it's handled
            // globally by the DockerClientFactory.
            DockerClient dockerClient = DockerClientFactory.lazyClient();
            return dockerClient.listContainersCmd()
                    .withLabelFilter(Map.of(DevServiceLabels.NAME, serviceName, DevServiceLabels.SHARED, "true"))
                    // Paused or restarting containers are never valid candidates.
                    .withStatusFilter(List.of(STATUS_RUNNING))
                    .exec()
                    .stream()
                    .filter(dockerContainer -> !startedByThisApplication(dockerContainer))
                    // Pick the oldest candidate so that applications starting concurrently
                    // converge deterministically on the same container. Creation timestamps
                    // have second granularity, so ties are broken by container ID.
                    .min(Comparator.<Container>comparingLong(dockerContainer ->
                            dockerContainer.getCreated() != null ? dockerContainer.getCreated() : Long.MAX_VALUE)
                            .thenComparing(dockerContainer ->
                                    dockerContainer.getId() != null ? dockerContainer.getId() : ""))
                    .map(dockerContainer -> new DiscoveredContainer(ContainerRuntimeInfo.toContainerInfo(dockerContainer),
                            DockerClientFactory.instance().dockerHostIpAddress()))
                    .orElse(null);
        } catch (Exception ex) {
            logger.info("Failed to look up shared containers for the '{}' dev service. Starting a dedicated container instead.", serviceName, ex);
            return null;
        }
    }

    /**
     * Whether the container was started by the application performing the lookup.
     * <p>
     * Such a container is never a candidate, and there is deliberately no way to
     * turn this check off: an application that adopts its own container would report
     * a dev service as discovered while still owning its lifecycle.
     */
    private static boolean startedByThisApplication(Container dockerContainer) {
        return dockerContainer.getLabels() != null
                && DevServiceLabels.ownerId().equals(dockerContainer.getLabels().get(DevServiceLabels.OWNER));
    }

}
