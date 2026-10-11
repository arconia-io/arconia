package io.arconia.dev.services.api.registration;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Describes a registered dev service.
 *
 * @param name the logical name of the dev service
 * @param description the description of the dev service
 * @param origin how the dev service container was made available to the application
 * @param containerInfo supplies the information about the container, inspected on each call
 * @param links the links the dev service exposes
 */
@Incubating
public record DevServiceRegistration(
        String name,
        String description,
        Origin origin,
        Supplier<ContainerInfo> containerInfo,
        List<DevServiceLink> links
) {

    public DevServiceRegistration {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.notNull(origin, "origin cannot be null");
        Assert.notNull(containerInfo, "containerInfo cannot be null");
        Assert.notNull(links, "links cannot be null");

        links = List.copyOf(links);
    }

    /**
     * How the dev service container was made available to the application.
     */
    public enum Origin {

        /**
         * The container is managed by this application through Testcontainers. The application
         * started it or attached to it when it is a container reused by Testcontainers.
         */
        MANAGED,

        /**
         * The container was started by another application and discovered
         * by this one, which only connects to it.
         */
        DISCOVERED

    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DevServiceRegistration}, with no links by default.
     */
    public static final class Builder {

        @Nullable
        private String name;
        @Nullable
        private String description;
        @Nullable
        private Origin origin;
        @Nullable
        private Supplier<ContainerInfo> containerInfo;
        private List<DevServiceLink> links = List.of();

        private Builder() {}

        /**
         * The logical name of the dev service.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * The description of the dev service.
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * How the dev service container was made available to the application.
         */
        public Builder origin(Origin origin) {
            this.origin = origin;
            return this;
        }

        /**
         * The supplier of the information about the container, inspected on each call.
         */
        public Builder containerInfo(Supplier<ContainerInfo> containerInfo) {
            this.containerInfo = containerInfo;
            return this;
        }

        /**
         * The links the dev service exposes.
         */
        public Builder links(List<DevServiceLink> links) {
            this.links = links;
            return this;
        }

        /**
         * Builds the {@link DevServiceRegistration} instance.
         */
        public DevServiceRegistration build() {
            Assert.notNull(name, "name cannot be null");
            Assert.notNull(description, "description cannot be null");
            Assert.notNull(origin, "origin cannot be null");
            Assert.notNull(containerInfo, "containerInfo cannot be null");
            return new DevServiceRegistration(name, description, origin, containerInfo, links);
        }

    }

}
