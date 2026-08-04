package io.arconia.dev.services.api.registration;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Describes a registered dev service.
 */
@Incubating
public record DevServiceRegistration(
        String name,
        @Nullable
        String description,
        Origin origin,
        Supplier<ContainerInfo> containerInfo,
        List<DevServiceLink> links
) {

    public DevServiceRegistration {
        Assert.hasText(name, "name cannot be null or empty");
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
         * The container was started and is managed by this application.
         */
        OWNED,

        /**
         * The container was started by another application and discovered
         * as a shared dev service.
         */
        DISCOVERED

    }

    public static Builder builder() {
        return new Builder();
    }

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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder origin(Origin origin) {
            this.origin = origin;
            return this;
        }

        public Builder containerInfo(Supplier<ContainerInfo> containerInfo) {
            this.containerInfo = containerInfo;
            return this;
        }

        public Builder links(List<DevServiceLink> links) {
            this.links = links;
            return this;
        }

        public DevServiceRegistration build() {
            return new DevServiceRegistration(name, description, origin, containerInfo, links);
        }

    }

}
