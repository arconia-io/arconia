package io.arconia.dev.services.core.registration;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;

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
    private BaseDevServicesProperties properties;

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
     * The configuration properties of the dev service.
     * <p>
     * The registry reads from them every configured value it needs, such as whether the
     * dev service is shared, so that no value has to be declared twice. Values that cannot
     * be derived from configuration, such as the container type or the connection details
     * contribution, are declared on this specification instead.
     */
    public ServiceSpec properties(BaseDevServicesProperties properties) {
        this.properties = properties;
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
    BaseDevServicesProperties getProperties() {
        return properties;
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
