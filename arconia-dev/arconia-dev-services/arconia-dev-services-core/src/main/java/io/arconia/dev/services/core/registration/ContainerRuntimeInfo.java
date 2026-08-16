package io.arconia.dev.services.core.registration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import org.testcontainers.DockerClientFactory;

import io.arconia.dev.services.api.registration.ContainerInfo;

/**
 * Reads container details from the OCI runtime and maps them to {@link ContainerInfo}.
 */
final class ContainerRuntimeInfo {

    private ContainerRuntimeInfo() {}

    /**
     * Extract container information by querying the OCI runtime using the container ID.
     */
    static ContainerInfo extractContainerInfoById(String containerId) {
        try {
            // Get Docker client from Testcontainers. We don't close the connection as it's handled
            // globally by the DockerClientFactory.
            DockerClient dockerClient = DockerClientFactory.lazyClient();
            // Query Docker for the container using its ID
            Container dockerContainer = dockerClient.listContainersCmd()
                    .withIdFilter(Collections.singleton(containerId))
                    .withShowAll(true)
                    .exec()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Container not found with ID: " + containerId));
            return toContainerInfo(dockerContainer);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to extract container information for ID: " + containerId, ex);
        }
    }

    /**
     * Map container details from the OCI runtime API to a {@link ContainerInfo}.
     */
    static ContainerInfo toContainerInfo(Container dockerContainer) {
        List<String> names = Arrays.stream(dockerContainer.getNames() != null ? dockerContainer.getNames() : new String[0])
                .map(name -> name.startsWith("/") ? name.substring(1) : name)
                .toList();
        String imageName = dockerContainer.getImage();
        Map<String, String> labels = dockerContainer.getLabels() != null ? dockerContainer.getLabels() : Map.of();
        String status = dockerContainer.getStatus();

        List<ContainerInfo.ContainerPort> exposedPorts = Arrays.stream(
                        dockerContainer.getPorts() != null ? dockerContainer.getPorts() : new ContainerPort[0])
                .map(port -> new ContainerInfo.ContainerPort(
                        port.getIp(),
                        port.getPrivatePort(),
                        port.getPublicPort(),
                        port.getType()
                ))
                .toList();
        return new ContainerInfo(dockerContainer.getId(), imageName, names, exposedPorts, labels, status);
    }

}
