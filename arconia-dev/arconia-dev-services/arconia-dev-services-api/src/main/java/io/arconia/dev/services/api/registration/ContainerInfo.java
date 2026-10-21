package io.arconia.dev.services.api.registration;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Holds information about a container.
 * <p>
 * Used to map details from <a href="https://github.com/docker-java/docker-java/blob/main/docker-java-api/src/main/java/com/github/dockerjava/api/model/Container.java">docker-java</a>.
 */
@Incubating
public final class ContainerInfo {

    private final String id;

    private final String imageName;

    private final List<String> names;

    private final List<ContainerPort> ports;

    private final Map<String, String> labels;

    private final String state;

    private final String status;

    private ContainerInfo(Builder builder) {
        Assert.hasText(builder.id, "id cannot be null or empty");
        Assert.hasText(builder.imageName, "imageName cannot be null or empty");
        Assert.notNull(builder.names, "names cannot be null");
        Assert.notNull(builder.ports, "ports cannot be null");
        Assert.notNull(builder.labels, "labels cannot be null");
        Assert.hasText(builder.state, "state cannot be null or empty");
        Assert.hasText(builder.status, "status cannot be null or empty");
        this.id = builder.id;
        this.imageName = builder.imageName;
        this.names = List.copyOf(builder.names);
        this.ports = List.copyOf(builder.ports);
        this.labels = Map.copyOf(builder.labels);
        this.state = builder.state;
        this.status = builder.status;
    }

    /**
     * The container id.
     */
    public String getId() {
        return id;
    }

    /**
     * The name of the container image.
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * The names of the container.
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * The port mappings of the container, as the container runtime reports them.
     */
    public List<ContainerPort> getPorts() {
        return ports;
    }

    /**
     * The labels of the container.
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * The state of the container as the container runtime reports it, one of the values
     * defined by the Docker Engine API ({@code created}, {@code running}, {@code paused},
     * {@code restarting}, {@code removing}, {@code exited}, {@code dead}).
     */
    public String getState() {
        return state;
    }

    /**
     * The human-readable status of the container as the container runtime reports it,
     * such as {@code Up 5 minutes}.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Holds information about a port mapping of a container.
     * <p>
     * Used to map details from <a href="https://github.com/docker-java/docker-java/blob/main/docker-java-api/src/main/java/com/github/dockerjava/api/model/ContainerPort.java">docker-java</a>.
     *
     * @param ip the host IP address the port is published on, if published
     * @param privatePort the port inside the container
     * @param publicPort the port on the host the container port is published to, if published
     * @param type the protocol of the port, such as {@code tcp} or {@code udp}
     */
    @Incubating
    public record ContainerPort(
            @Nullable String ip,
            @Nullable Integer privatePort,
            @Nullable Integer publicPort,
            @Nullable String type
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private String id;
        @Nullable
        private String imageName;
        private List<String> names = List.of();
        private List<ContainerPort> ports = List.of();
        private Map<String, String> labels = Map.of();
        @Nullable
        private String state;
        @Nullable
        private String status;

        private Builder() {}

        /**
         * The container id.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The image name of the container.
         */
        public Builder imageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        /**
         * The names of the container.
         */
        public Builder names(List<String> names) {
            this.names = names;
            return this;
        }

        /**
         * The port mappings of the container.
         */
        public Builder ports(List<ContainerPort> ports) {
            this.ports = ports;
            return this;
        }

        /**
         * The labels of the container.
         */
        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        /**
         * The state of the container, such as {@code running} or {@code exited}.
         */
        public Builder state(String state) {
            this.state = state;
            return this;
        }

        /**
         * The human-readable status of the container, such as {@code Up 5 minutes}.
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Builds the {@link ContainerInfo} instance.
         */
        public ContainerInfo build() {
            return new ContainerInfo(this);
        }

    }

}
