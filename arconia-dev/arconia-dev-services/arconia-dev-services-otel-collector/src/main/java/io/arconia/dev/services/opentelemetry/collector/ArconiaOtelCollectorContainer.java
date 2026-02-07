package io.arconia.dev.services.opentelemetry.collector;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An OpenTelemetry Collector {@link Container} configured for use with Arconia Dev Services.
 */
final class ArconiaOtelCollectorContainer extends GenericContainer<ArconiaOtelCollectorContainer> {

    private static final String COMPATIBLE_IMAGE_NAME = "otel/opentelemetry-collector-contrib";

    private final OtelCollectorDevServicesProperties properties;

    static final int OTLP_GRPC_PORT = 4317;

    static final int OTLP_HTTP_PORT = 4318;

    public ArconiaOtelCollectorContainer(OtelCollectorDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        addExposedPorts(OTLP_GRPC_PORT, OTLP_HTTP_PORT);
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), OTLP_HTTP_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getOtlpGrpcPort())) {
            addFixedExposedPort(properties.getOtlpGrpcPort(), OTLP_GRPC_PORT);
        }
    }

    public Integer getGrpcPort() {
        return getMappedPort(OTLP_GRPC_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(OTLP_HTTP_PORT);
    }

}
