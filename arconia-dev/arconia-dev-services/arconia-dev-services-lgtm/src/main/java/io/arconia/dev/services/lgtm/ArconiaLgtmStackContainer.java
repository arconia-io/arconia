package io.arconia.dev.services.lgtm;

import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link LgtmStackContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaLgtmStackContainer extends LgtmStackContainer {

    private final LgtmDevServicesProperties properties;

    /**
     * Grafana web UI port.
     */
    protected static final int GRAFANA_PORT = 3000;

    /**
     * Loki HTTP API port.
     */
    protected static final int LOKI_PORT = 3100;

    /**
     * OTLP receiver port (gRPC).
     */
    protected static final int OTLP_GRPC_PORT = 4317;

    /**
     * OTLP receiver port (HTTP).
     */
    protected static final int OTLP_HTTP_PORT = 4318;

    public ArconiaLgtmStackContainer(DockerImageName image, LgtmDevServicesProperties properties) {
        super(image);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), GRAFANA_PORT);
        }
    }

}
