package io.arconia.dev.services.core.registration;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import io.arconia.dev.services.api.registration.DevServiceLabels;

/**
 * Discovers dev service containers by querying the OCI runtime for the labels
 * Arconia applies when starting a discoverable container.
 */
final class DockerContainerDiscovery implements ContainerDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(DockerContainerDiscovery.class);

    private static final String STATUS_RUNNING = "running";

    @Override
    public List<DiscoveredContainer> discover(String serviceName) {
        try {
            // Get Docker client from Testcontainers. We don't close the connection as it's handled
            // globally by the DockerClientFactory.
            DockerClient dockerClient = DockerClientFactory.lazyClient();
            List<Container> containers = dockerClient.listContainersCmd()
                    .withLabelFilter(Map.of(DevServiceLabels.NAME, serviceName, DevServiceLabels.DISCOVERABLE, "true"))
                    // Paused or restarting containers are never valid candidates.
                    .withStatusFilter(List.of(STATUS_RUNNING))
                    .exec();
            return select(containers, DockerClientFactory.instance().dockerHostIpAddress(), serviceName);
        } catch (Exception ex) {
            logger.info("Failed to discover running containers for the '{}' dev service. Starting its own container instead.", serviceName);
            logger.debug("Container discovery failure for the '{}' dev service", serviceName, ex);
            return List.of();
        }
    }

    /**
     * Select the containers that can be used among the ones the container runtime
     * reports for the dev service, leaving out the ones started by this application.
     * <p>
     * The candidates are ordered oldest first, so that applications starting concurrently
     * converge deterministically on the same container. Creation timestamps have "second"
     * granularity, so ties are broken by container ID.
     */
    static List<DiscoveredContainer> select(List<Container> containers, String host, String serviceName) {
        return containers.stream()
                .filter(dockerContainer -> !startedByThisApplication(dockerContainer))
                .sorted(Comparator.<Container>comparingLong(dockerContainer -> dockerContainer.getCreated() != null ? dockerContainer.getCreated() : Long.MAX_VALUE)
                        .thenComparing(dockerContainer -> dockerContainer.getId() != null ? dockerContainer.getId() : ""))
                .map(dockerContainer -> toDiscoveredContainer(dockerContainer, host, serviceName))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Whether the container was started by the application performing the discovery.
     */
    private static boolean startedByThisApplication(Container dockerContainer) {
        return dockerContainer.getLabels() != null
                && DevServiceLabels.ownerId().equals(dockerContainer.getLabels().get(DevServiceLabels.OWNER));
    }

    /**
     * Describe a candidate container for a dev service.
     */
    @Nullable
    private static DiscoveredContainer toDiscoveredContainer(Container dockerContainer, String host, String serviceName) {
        try {
            return new DiscoveredContainer(ContainerRuntimeInfo.toContainerInfo(dockerContainer), host);
        } catch (Exception ex) {
            logger.debug("Skipping container {} discovered for the '{}' dev service", dockerContainer.getId(), serviceName, ex);
            return null;
        }
    }

}
