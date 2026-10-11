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
 *
 * @param id the container id
 * @param imageName the name of the container image
 * @param names the names of the container
 * @param exposedPorts the ports the container exposes
 * @param labels the labels of the container
 * @param state the state of the container as the container runtime reports it, one of the values
 *              defined by the Docker Engine API ({@code created}, {@code running}, {@code paused},
 *              {@code restarting}, {@code removing}, {@code exited}, {@code dead})
 * @param status the human-readable status of the container as the container runtime reports it,
 *               such as {@code Up 5 minutes}
 */
@Incubating
public record ContainerInfo(
        String id,
        String imageName,
        List<String> names,
        List<ContainerPort> exposedPorts,
        Map<String, String> labels,
        String state,
        String status
) {

    public ContainerInfo {
        Assert.hasText(id, "id cannot be null or empty");
        Assert.hasText(imageName, "imageName cannot be null or empty");
        Assert.notNull(names, "names cannot be null");
        Assert.notNull(exposedPorts, "exposedPorts cannot be null");
        Assert.notNull(labels, "labels cannot be null");
        Assert.hasText(state, "state cannot be null or empty");
        Assert.hasText(status, "status cannot be null or empty");

        names = List.copyOf(names);
        exposedPorts = List.copyOf(exposedPorts);
        labels = Map.copyOf(labels);
    }

    /**
     * Holds information about a container port.
     * <p>
     * Used to map details from <a href="https://github.com/docker-java/docker-java/blob/main/docker-java-api/src/main/java/com/github/dockerjava/api/model/ContainerPort.java">docker-java</a>.
     */
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
        private List<ContainerPort> exposedPorts = List.of();
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
         * The exposed ports of the container.
         */
        public Builder exposedPorts(List<ContainerPort> exposedPorts) {
            this.exposedPorts = exposedPorts;
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
            Assert.notNull(id, "id cannot be null");
            Assert.notNull(imageName, "imageName cannot be null");
            Assert.notNull(state, "state cannot be null");
            Assert.notNull(status, "status cannot be null");
            return new ContainerInfo(id, imageName, names, exposedPorts, labels, state, status);
        }

    }

}
