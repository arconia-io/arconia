package io.arconia.dev.services.core.registration;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;

import org.jspecify.annotations.Nullable;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;

import io.arconia.dev.services.api.registration.ContainerInfo;

/**
 * Reads container details from the OCI runtime and maps them to {@link ContainerInfo}.
 */
final class ContainerRuntimeInfo {

    private ContainerRuntimeInfo() {}

    /**
     * The container id in the short form used by the container runtime CLIs.
     */
    static String shortId(@Nullable String containerId) {
        if (containerId == null) {
            return "<none>";
        }
        return (containerId.length() > 12) ? containerId.substring(0, 12) : containerId;
    }

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
     * The content of a file of the given container, read through the OCI runtime with the
     * Testcontainers utility, which only needs to know the container.
     */
    static byte[] copyFileFromContainer(String containerId, String containerPath) {
        try {
            InspectContainerResponse containerInfo = DockerClientFactory.lazyClient().inspectContainerCmd(containerId).exec();
            return new RunningContainerState(containerInfo).copyFileFromContainer(containerPath, InputStream::readAllBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to copy the file %s from the container with ID: %s".formatted(containerPath, containerId), ex);
        }
    }

    /**
     * A running container the application did not start, as the state Testcontainers operates
     * on: enough for the utilities that only need the container itself.
     */
    private record RunningContainerState(InspectContainerResponse containerInfo) implements ContainerState {

        @Override
        public InspectContainerResponse getContainerInfo() {
            return containerInfo;
        }

        @Override
        public List<Integer> getExposedPorts() {
            return List.of();
        }

    }

    /**
     * Map container details from the OCI runtime API to a {@link ContainerInfo}.
     */
    static ContainerInfo toContainerInfo(Container dockerContainer) {
        List<String> names = Arrays.stream(dockerContainer.getNames() != null ? dockerContainer.getNames() : new String[0])
                .map(name -> name.startsWith("/") ? name.substring(1) : name)
                .toList();
        List<ContainerInfo.ContainerPort> ports = Arrays.stream(
                        dockerContainer.getPorts() != null ? dockerContainer.getPorts() : new ContainerPort[0])
                .map(port -> new ContainerInfo.ContainerPort(
                        port.getIp(),
                        port.getPrivatePort(),
                        port.getPublicPort(),
                        port.getType()
                ))
                .toList();
        return ContainerInfo.builder()
                .id(dockerContainer.getId())
                .imageName(dockerContainer.getImage())
                .names(names)
                .ports(ports)
                .labels(dockerContainer.getLabels() != null ? dockerContainer.getLabels() : Map.of())
                .state(dockerContainer.getState())
                .status(dockerContainer.getStatus())
                .build();
    }

}
