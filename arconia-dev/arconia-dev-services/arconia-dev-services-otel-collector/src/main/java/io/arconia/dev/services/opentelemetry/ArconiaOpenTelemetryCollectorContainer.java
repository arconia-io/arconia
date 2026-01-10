package io.arconia.dev.services.opentelemetry;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An OpenTelemetry Collector {@link Container} specialized for Arconia Dev Services.
 */
public final class ArconiaOpenTelemetryCollectorContainer extends GenericContainer<ArconiaOpenTelemetryCollectorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("otel/opentelemetry-collector-contrib");

    private static final int DEFAULT_GRPC_PORT = 4317;

    private static final int DEFAULT_HTTP_PORT = 4318;

    public ArconiaOpenTelemetryCollectorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(DEFAULT_GRPC_PORT, DEFAULT_HTTP_PORT);
    }

    public Integer getGrpcPort() {
        return getMappedPort(DEFAULT_GRPC_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(DEFAULT_HTTP_PORT);
    }

}
