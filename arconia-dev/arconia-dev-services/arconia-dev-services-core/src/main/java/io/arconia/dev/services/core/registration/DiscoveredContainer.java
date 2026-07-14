package io.arconia.dev.services.core.registration;

import java.util.Comparator;
import java.util.Objects;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.registration.ContainerInfo;

/**
 * A running shared dev service container discovered from another application,
 * providing the information needed to build the connection configuration.
 *
 * @param containerInfo information about the discovered container
 * @param host the host address to use for connecting to the discovered container
 */
@Incubating
public record DiscoveredContainer(
        ContainerInfo containerInfo,
        String host
) {

    public DiscoveredContainer {
        Assert.notNull(containerInfo, "containerInfo cannot be null");
        Assert.hasText(host, "host cannot be null or empty");
    }

    /**
     * The host port to which the given container port is mapped over TCP.
     * On dual-stack hosts reporting one mapping per IP family, the IPv4 binding wins,
     * matching the address family of {@link #host()}.
     * @throws IllegalStateException if the given container port is not mapped to any host port
     */
    public int mappedPort(int containerPort) {
        return containerInfo.exposedPorts().stream()
                .filter(port -> port.privatePort() != null && port.privatePort() == containerPort)
                // A UDP mapping on the same port number is a different endpoint.
                .filter(port -> port.type() == null || "tcp".equalsIgnoreCase(port.type()))
                .filter(port -> Objects.nonNull(port.publicPort()))
                // On dual-stack hosts, the same mapping is reported once per IP family and the
                // host ports can differ. Prefer the IPv4 binding to match the resolved host address.
                .min(Comparator.comparing(port -> port.ip() != null && port.ip().contains(":")))
                .map(ContainerInfo.ContainerPort::publicPort)
                .orElseThrow(() -> new IllegalStateException("No host port mapped to container port %d for container %s"
                        .formatted(containerPort, containerInfo.id())));
    }

}
