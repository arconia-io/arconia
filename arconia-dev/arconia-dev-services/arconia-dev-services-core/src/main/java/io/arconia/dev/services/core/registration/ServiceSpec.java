package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.registration.DevServiceLink;

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
    private Class<? extends GenericContainer<?>> containerType;

    @Nullable
    private Supplier<? extends GenericContainer<?>> containerSupplier;

    private boolean serviceConnection = true;

    @Nullable
    private String serviceConnectionName;

    @Nullable
    private Class<? extends ConnectionDetails> connectionDetailsType;

    @Nullable
    private Function<DiscoveredContainer, ? extends ConnectionDetails> connectionDetails;

    private final List<LinkDefinition> links = new ArrayList<>();

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
     */
    public ServiceSpec properties(BaseDevServicesProperties properties) {
        this.properties = properties;
        return this;
    }

    /**
     * The container to register for the dev service: its type and a supplier
     * function providing the instance.
     */
    public <C extends GenericContainer<?>> ServiceSpec container(Class<C> type, Supplier<? extends C> supplier) {
        this.containerType = type;
        this.containerSupplier = supplier;
        return this;
    }

    /**
     * Whether the {@link ServiceConnection} annotation is added to the registered container
     * bean, for Spring Boot to provide the connection details for the container. Enabled by default.
     * <p>
     * Disable it when no {@code ContainerConnectionDetailsFactory} is available for the
     * container, since Spring Boot fails to start an application having a service
     * connection that provides no connection details. In that case, the application is
     * connected to the dev service via properties instead (see {@link DevServiceDynamicPropertySource}).
     */
    public ServiceSpec serviceConnection(boolean serviceConnection) {
        this.serviceConnection = serviceConnection;
        return this;
    }

    /**
     * The name of the {@link ServiceConnection} annotation added to the registered container bean.
     * <p>
     * By default, the annotation is added with no explicit name, and Spring Boot determines
     * the connection details to provide from the container type. Set a name when they are
     * determined from the name of the service instead, which is the case when no
     * technology-specific container type is available or supported by Spring Boot.
     */
    public ServiceSpec serviceConnectionName(String serviceConnectionName) {
        Assert.hasText(serviceConnectionName, "serviceConnectionName cannot be null or empty");
        this.serviceConnectionName = serviceConnectionName;
        return this;
    }

    /**
     * How to connect to the dev service running in a container started by another
     * application: the type of {@link ConnectionDetails} provided for the dev service and a
     * factory function providing the instance for a container discovered as running.
     * <p>
     * With the {@code framework} reuse strategy, the dev service container is discoverable by
     * other applications. The application connects to a running container (used as it
     * runs, with the configuration of the application that started it) if one is discovered,
     * instead of starting a new one. A dev service that declares no discovery never participates
     * in it, even when the {@code framework} reuse strategy is configured.
     * <p>
     * The declared type is used to look up existing user-defined {@code ConnectionDetails}
     * beans: when one is present, it takes precedence over the one provided by the dev service.
     */
    public <D extends ConnectionDetails> ServiceSpec discovery(Class<D> connectionDetailsType, Function<DiscoveredContainer, ? extends D> connectionDetails) {
        Assert.state(this.connectionDetails == null, "discovery supports a single connection details contribution");
        this.connectionDetailsType = connectionDetailsType;
        this.connectionDetails = connectionDetails;
        return this;
    }

    /**
     * A link the dev service exposes, such as a management console or a telemetry endpoint,
     * at the given container port. The link is resolved against the port mapping of the
     * container the dev service runs in, whether started by this application or discovered,
     * and shown in the startup message and in developer tooling.
     */
    public ServiceSpec link(String label, int port) {
        return link(label, port, "");
    }

    /**
     * A link the dev service exposes at the given container port and path.
     */
    public ServiceSpec link(String label, int port, String path) {
        links.add(new LinkDefinition(label, port, path));
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
    Class<? extends GenericContainer<?>> getContainerType() {
        return containerType;
    }

    @Nullable
    Supplier<? extends GenericContainer<?>> getContainerSupplier() {
        return containerSupplier;
    }

    boolean isServiceConnection() {
        return serviceConnection;
    }

    @Nullable
    String getServiceConnectionName() {
        return serviceConnectionName;
    }

    @Nullable
    Class<? extends ConnectionDetails> getConnectionDetailsType() {
        return connectionDetailsType;
    }

    @Nullable
    Function<DiscoveredContainer, ? extends ConnectionDetails> getConnectionDetails() {
        return connectionDetails;
    }

    List<LinkDefinition> getLinks() {
        return List.copyOf(links);
    }

    /**
     * Whether the dev service declares how to connect to a container started by another application.
     */
    boolean supportsDiscovery() {
        return connectionDetailsType != null && connectionDetails != null;
    }

    /**
     * A link declared in terms of the container port it points to, resolved into a
     * {@link DevServiceLink} once the port mapping is known.
     */
    record LinkDefinition(String label, int port, String path) {

        LinkDefinition {
            Assert.hasText(label, "label cannot be null or empty");
            Assert.isTrue(port > 0 && port <= 65535, "port must be between 1 and 65535");
            Assert.notNull(path, "path cannot be null");
            Assert.isTrue(path.isEmpty() || path.startsWith("/"), "path must be empty or start with '/': " + path);
        }

        DevServiceLink resolve(String host, int mappedPort) {
            return new DevServiceLink(label, "http://%s:%d%s".formatted(host, mappedPort, path));
        }

    }

}
