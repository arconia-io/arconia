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
public final class DevServiceRegistration {

    private final String name;

    private final String description;

    @Nullable
    private final String category;

    private final Origin origin;

    private final Supplier<ContainerInfo> containerInfo;

    private final List<DevServiceLink> links;

    @Nullable
    private final String sslBundleName;

    private DevServiceRegistration(Builder builder) {
        Assert.hasText(builder.name, "name cannot be null or empty");
        Assert.hasText(builder.description, "description cannot be null or empty");
        Assert.isTrue(builder.category == null || !builder.category.isBlank(), "category cannot be empty");
        Assert.notNull(builder.origin, "origin cannot be null");
        Assert.notNull(builder.containerInfo, "containerInfo cannot be null");
        Assert.notNull(builder.links, "links cannot be null");
        this.name = builder.name;
        this.description = builder.description;
        this.category = builder.category;
        this.origin = builder.origin;
        this.containerInfo = builder.containerInfo;
        this.links = List.copyOf(builder.links);
        this.sslBundleName = builder.sslBundleName;
    }

    /**
     * The logical name of the dev service.
     */
    public String getName() {
        return name;
    }

    /**
     * The description of the dev service.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The category of mutually exclusive dev services the dev service belongs to, if any.
     */
    @Nullable
    public String getCategory() {
        return category;
    }

    /**
     * How the dev service container was made available to the application.
     */
    public Origin getOrigin() {
        return origin;
    }

    /**
     * Supplies the information about the container, inspected on each call.
     */
    public Supplier<ContainerInfo> getContainerInfo() {
        return containerInfo;
    }

    /**
     * The links the dev service exposes.
     */
    public List<DevServiceLink> getLinks() {
        return links;
    }

    /**
     * The name of the SSL bundle the application connects to the dev service with, registered
     * in the {@code SslBundles} of the application, when SSL is enabled with a configured
     * certificate.
     */
    @Nullable
    public String getSslBundleName() {
        return sslBundleName;
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
        private String category;
        @Nullable
        private Origin origin;
        @Nullable
        private Supplier<ContainerInfo> containerInfo;
        private List<DevServiceLink> links = List.of();
        @Nullable
        private String sslBundleName;

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
         * The category of mutually exclusive dev services the dev service belongs to, if any.
         */
        public Builder category(@Nullable String category) {
            this.category = category;
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
         * The name of the SSL bundle the application connects to the dev service with, if any.
         */
        public Builder sslBundleName(@Nullable String sslBundleName) {
            this.sslBundleName = sslBundleName;
            return this;
        }

        /**
         * Builds the {@link DevServiceRegistration} instance.
         */
        public DevServiceRegistration build() {
            return new DevServiceRegistration(this);
        }

    }

}
