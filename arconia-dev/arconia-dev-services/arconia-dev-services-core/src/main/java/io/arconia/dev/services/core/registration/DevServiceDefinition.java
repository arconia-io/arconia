package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.registration.DevServiceLink;

/**
 * The complete and validated definition of a dev service, as built from a {@link DevServiceSpec}.
 *
 * @param name the logical name of the dev service
 * @param description the description of the dev service
 * @param category the category of mutually exclusive dev services it belongs to, if any
 * @param properties the configuration properties of the dev service
 * @param containerType the type of the container the dev service runs in
 * @param containerSupplier the supplier providing the container instance
 * @param serviceConnection whether the container bean carries the {@code @ServiceConnection} annotation
 * @param serviceConnectionName the name of the service connection, if an explicit one is needed
 * @param discovery how to connect to a container started by another application, if declared
 * @param port the container port the common {@code port} property binds to the host, if declared
 * @param additionalPorts the other container ports bound to fixed host ports
 * @param links the links the dev service exposes
 */
record DevServiceDefinition(
        String name,
        String description,
        @Nullable String category,
        BaseDevServicesProperties properties,
        Class<? extends GenericContainer<?>> containerType,
        Supplier<? extends GenericContainer<?>> containerSupplier,
        boolean serviceConnection,
        @Nullable String serviceConnectionName,
        @Nullable Discovery discovery,
        @Nullable Integer port,
        List<PortBinding> additionalPorts,
        List<LinkDefinition> links
) {

    DevServiceDefinition {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.notNull(properties, "properties cannot be null");
        Assert.notNull(containerType, "containerType cannot be null");
        Assert.notNull(containerSupplier, "containerSupplier cannot be null");
        Assert.notNull(additionalPorts, "additionalPorts cannot be null");
        Assert.notNull(links, "links cannot be null");

        additionalPorts = List.copyOf(additionalPorts);
        links = List.copyOf(links);
    }

    /**
     * A container port bound to a fixed port on the host. A host port of 0 binds nothing,
     * which is how a fixed-port property reads when it is not set.
     */
    record PortBinding(int containerPort, int hostPort) {}

    /**
     * How to connect to the dev service running in a container started by another application:
     * the type of {@link ConnectionDetails} provided and the factory building it for a discovered container.
     */
    record Discovery(
            Class<? extends ConnectionDetails> type,
            Function<DiscoveredContainer, ? extends ConnectionDetails> factory
    ) {

        Discovery {
            Assert.notNull(type, "type cannot be null");
            Assert.notNull(factory, "factory cannot be null");
        }

    }

    /**
     * A link declared in terms of the container port it points to, resolved into a
     * {@link DevServiceLink} once the port mapping is known.
     */
    record LinkDefinition(String label, int port, String path) {

        LinkDefinition {
            Assert.notNull(label, "label cannot be null");
            Assert.notNull(path, "path cannot be null");
        }

        DevServiceLink resolve(String host, int mappedPort) {
            return new DevServiceLink(label, "http://%s:%d%s".formatted(host, mappedPort, path));
        }

    }

}
