package io.arconia.dev.services.opentelemetry.collector;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


/**
 * An OpenTelemetry Collector {@link Container} configured for use with Arconia Dev Services.
 */
final class ArconiaOtelCollectorContainer extends GenericContainer<ArconiaOtelCollectorContainer> {

    static final String COMPATIBLE_IMAGE_NAME = "otel/opentelemetry-collector-contrib";

    static final int OTLP_GRPC_PORT = 4317;

    static final int OTLP_HTTP_PORT = 4318;

    public ArconiaOtelCollectorContainer(OtelCollectorDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        addExposedPorts(OTLP_GRPC_PORT, OTLP_HTTP_PORT);

        // The collector logs this line once its pipelines are running. The image has no shell,
        // so the default wait strategy, which checks the ports from inside the container, can't be used.
        this.waitingFor(Wait.forLogMessage(".*Everything is ready\\. Begin running and processing data\\..*", 1));
    }

    public Integer getGrpcPort() {
        return getMappedPort(OTLP_GRPC_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(OTLP_HTTP_PORT);
    }

}
