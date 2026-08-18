package io.arconia.dev.services.opentelemetry.collector;

import java.util.List;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An OpenTelemetry Collector {@link Container} configured for use with Arconia Dev Services.
 */
final class ArconiaOtelCollectorContainer extends GenericContainer<ArconiaOtelCollectorContainer> implements DevServiceLinkProvider {

    private final OtelCollectorDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "otel/opentelemetry-collector-contrib";

    static final int OTLP_GRPC_PORT = 4317;

    static final int OTLP_HTTP_PORT = 4318;

    public ArconiaOtelCollectorContainer(OtelCollectorDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        addExposedPorts(OTLP_GRPC_PORT, OTLP_HTTP_PORT);
        this.properties = properties;

        // Testcontainers uses a shared wait strategy instance across all containers.
        // GenericContainer doesn't set a wait strategy of its own, so when we customize
        // the startup timeout, it will be applied to all containers. Hence, we must
        // provide an explicit wait strategy.
        this.waitingFor(Wait.defaultWaitStrategy());

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), OTLP_HTTP_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getOtlpGrpcPort())) {
            addFixedExposedPort(properties.getOtlpGrpcPort(), OTLP_GRPC_PORT);
        }
    }

    public Integer getGrpcPort() {
        return getMappedPort(OTLP_GRPC_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(OTLP_HTTP_PORT);
    }

    @Override
    public List<DevServiceLinkDefinition> devServiceLinkDefinitions() {
        return List.of(
                DevServiceLinkDefinition.builder().id("otlp-grpc").label("OTLP/gRPC").port(OTLP_GRPC_PORT).build(),
                DevServiceLinkDefinition.builder().id("otlp-http").label("OTLP/HTTP").port(OTLP_HTTP_PORT).build());
    }

}
