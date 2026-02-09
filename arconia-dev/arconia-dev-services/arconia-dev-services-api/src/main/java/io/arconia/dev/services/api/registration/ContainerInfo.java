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
public record ContainerInfo(
        String id,
        String imageName,
        List<String> names,
        List<ContainerPort> exposedPorts,
        Map<String, String> labels,
        String status
) {

    public ContainerInfo {
        Assert.hasText(id, "id cannot be null or empty");
        Assert.hasText(imageName, "imageName cannot be null or empty");
        Assert.notNull(names, "names cannot be null");
        Assert.notNull(exposedPorts, "exposedPorts cannot be null");
        Assert.notNull(labels, "labels cannot be null");
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

        private String id;
        private String imageName;
        private List<String> names = List.of();
        private List<ContainerPort> exposedPorts = List.of();
        private Map<String, String> labels = Map.of();
        private String status;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder imageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public Builder names(List<String> names) {
            this.names = names;
            return this;
        }

        public Builder exposedPorts(List<ContainerPort> exposedPorts) {
            this.exposedPorts = exposedPorts;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public ContainerInfo build() {
            return new ContainerInfo(id, imageName, names, exposedPorts, labels, status);
        }

    }

}
