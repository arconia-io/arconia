package io.arconia.dev.services.core.registration;

import java.util.Comparator;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.registration.ContainerInfo;

/**
 * A running dev service container started by another application and discovered,
 * providing the information needed to build the connection configuration.
 */
@Incubating
public final class DiscoveredContainer {

    private final ContainerInfo containerInfo;

    private final String host;

    @Nullable
    private final SslBundle sslBundle;

    private DiscoveredContainer(Builder builder) {
        Assert.notNull(builder.containerInfo, "containerInfo cannot be null");
        Assert.hasText(builder.host, "host cannot be null or empty");
        this.containerInfo = builder.containerInfo;
        this.host = builder.host;
        this.sslBundle = builder.sslBundle;
    }

    /**
     * Information about the discovered container.
     */
    public ContainerInfo containerInfo() {
        return containerInfo;
    }

    /**
     * The host address to use for connecting to the discovered container.
     */
    public String host() {
        return host;
    }

    /**
     * The bundle to connect to the discovered container over SSL with, when SSL is enabled for
     * the dev service. It trusts the certificate the dev service is configured to present.
     */
    @Nullable
    public SslBundle sslBundle() {
        return sslBundle;
    }

    /**
     * The host port to which the given container port is mapped over TCP.
     * <p>
     * On dual-stack hosts reporting one mapping per IP family, the IPv4 binding wins,
     * matching the address family of {@link #host()}.
     */
    public int mappedPort(int containerPort) {
        return containerInfo.getPorts().stream()
                .filter(port -> port.privatePort() != null && port.privatePort() == containerPort)
                // A UDP mapping on the same port number is a different endpoint.
                .filter(port -> port.type() == null || "tcp".equalsIgnoreCase(port.type()))
                .filter(port -> Objects.nonNull(port.publicPort()))
                // On dual-stack hosts, the same mapping is reported once per IP family and the
                // host ports can differ. Prefer the IPv4 binding to match the resolved host address.
                .min(Comparator.comparing(port -> port.ip() != null && port.ip().contains(":")))
                .map(ContainerInfo.ContainerPort::publicPort)
                .orElseThrow(() -> new IllegalStateException("No host port mapped to container port %d for container %s"
                        .formatted(containerPort, containerInfo.getId())));
    }

    /**
     * The content of the file at the given path in the discovered container, read through the
     * container runtime, for a dev service to pick up what the service generated at startup,
     * such as a certificate.
     *
     * @throws IllegalStateException if the file cannot be read
     */
    public byte[] copyFileFromContainer(String containerPath) {
        Assert.hasText(containerPath, "containerPath cannot be null or empty");
        return ContainerRuntimeInfo.copyFileFromContainer(containerInfo.getId(), containerPath);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private ContainerInfo containerInfo;
        @Nullable
        private String host;
        @Nullable
        private SslBundle sslBundle;

        private Builder() {}

        /**
         * Information about the discovered container.
         */
        public Builder containerInfo(ContainerInfo containerInfo) {
            this.containerInfo = containerInfo;
            return this;
        }

        /**
         * The host address to use for connecting to the discovered container.
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * The bundle to connect to the discovered container over SSL with, if any.
         */
        public Builder sslBundle(@Nullable SslBundle sslBundle) {
            this.sslBundle = sslBundle;
            return this;
        }

        /**
         * Builds the {@link DiscoveredContainer} instance.
         */
        public DiscoveredContainer build() {
            return new DiscoveredContainer(this);
        }

    }

}
