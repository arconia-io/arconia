package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.SslDevServicesProperties;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.core.ssl.DevServiceSslConfigurer;
import io.arconia.dev.services.core.ssl.SslCertificate;

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
 * @param sslSupport how the container is configured to use SSL, if the dev service supports it
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
        List<LinkDefinition> links,
        @Nullable SslSupport sslSupport
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
     * Whether SSL is enabled for the dev service: it supports SSL, and its properties enable it.
     */
    boolean isSslEnabled() {
        return sslSupport != null
                && properties instanceof SslDevServicesProperties sslProperties
                && sslProperties.getSsl().isEnabled();
    }

    /**
     * How the container of a dev service is configured to use SSL, and where the certificate
     * it presents comes from.
     */
    record SslSupport(
            DevServiceSslConfigurer<? extends GenericContainer<?>> configurer,
            SslCertificate certificate
    ) {

        SslSupport {
            Assert.notNull(configurer, "configurer cannot be null");
            Assert.notNull(certificate, "certificate cannot be null");
        }

    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {

        @Nullable
        private String name;
        @Nullable
        private String description;
        @Nullable
        private String category;
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
        private Discovery discovery;
        @Nullable
        private Integer port;
        private List<PortBinding> additionalPorts = List.of();
        private List<LinkDefinition> links = List.of();
        @Nullable
        private SslSupport sslSupport;

        private Builder() {}

        Builder name(String name) {
            this.name = name;
            return this;
        }

        Builder description(String description) {
            this.description = description;
            return this;
        }

        Builder category(@Nullable String category) {
            this.category = category;
            return this;
        }

        Builder properties(BaseDevServicesProperties properties) {
            this.properties = properties;
            return this;
        }

        Builder container(Class<? extends GenericContainer<?>> type, Supplier<? extends GenericContainer<?>> supplier) {
            this.containerType = type;
            this.containerSupplier = supplier;
            return this;
        }

        Builder serviceConnection(boolean serviceConnection) {
            this.serviceConnection = serviceConnection;
            return this;
        }

        Builder serviceConnectionName(@Nullable String serviceConnectionName) {
            this.serviceConnectionName = serviceConnectionName;
            return this;
        }

        Builder discovery(@Nullable Discovery discovery) {
            this.discovery = discovery;
            return this;
        }

        Builder port(@Nullable Integer port) {
            this.port = port;
            return this;
        }

        Builder additionalPorts(List<PortBinding> additionalPorts) {
            this.additionalPorts = additionalPorts;
            return this;
        }

        Builder links(List<LinkDefinition> links) {
            this.links = links;
            return this;
        }

        Builder sslSupport(@Nullable SslSupport sslSupport) {
            this.sslSupport = sslSupport;
            return this;
        }

        DevServiceDefinition build() {
            Assert.notNull(name, "name cannot be null");
            Assert.notNull(description, "description cannot be null");
            Assert.notNull(properties, "properties cannot be null");
            Assert.notNull(containerType, "containerType cannot be null");
            Assert.notNull(containerSupplier, "containerSupplier cannot be null");
            return new DevServiceDefinition(name, description, category, properties, containerType, containerSupplier,
                    serviceConnection, serviceConnectionName, discovery, port, additionalPorts, links, sslSupport);
        }

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
