package io.arconia.dev.services.core.registration;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;

/**
 * Specification for a single dev service.
 */
@Incubating
public final class ServiceSpec {

    @Nullable
    private String name;

    @Nullable
    private String description;

    @Nullable
    private ContainerSpec containerSpec;

    @Nullable
    private DiscoverySpec discoverySpec;

    ServiceSpec() {}

    /**
     * The logical name of the dev service.
     */
    public ServiceSpec name(String name) {
        this.name = name;
        return this;
    }

    /**
     * The description of the dev service.
     */
    public ServiceSpec description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Specification for the container to register.
     */
    public ServiceSpec container(Consumer<ContainerSpec> containerSpecConsumer) {
        var containerSpec = new ContainerSpec();
        containerSpecConsumer.accept(containerSpec);
        this.containerSpec = containerSpec;
        return this;
    }

    /**
     * Specification for discovering and adopting a shared dev service running in a container
     * started by another application. A dev service that declares no discovery specification
     * never participates in discovery, even when the {@code shared} property is enabled.
     */
    public ServiceSpec discovery(Consumer<DiscoverySpec> discoverySpecConsumer) {
        var discoverySpec = new DiscoverySpec();
        discoverySpecConsumer.accept(discoverySpec);
        this.discoverySpec = discoverySpec;
        return this;
    }

    @Nullable
    String getName() {
        return name;
    }

    @Nullable
    String getDescription() {
        return description;
    }

    @Nullable
    ContainerSpec getContainerSpec() {
        return containerSpec;
    }

    @Nullable
    DiscoverySpec getDiscoverySpec() {
        return discoverySpec;
    }

}
