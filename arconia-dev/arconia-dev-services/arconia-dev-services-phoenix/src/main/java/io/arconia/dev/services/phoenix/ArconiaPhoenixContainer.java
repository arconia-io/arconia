package io.arconia.dev.services.phoenix;

import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * A {@link PhoenixContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPhoenixContainer extends PhoenixContainer {

    private final PhoenixDevServicesProperties properties;

    /**
     * Phoenix Web UI port.
     */
    protected static final int PHOENIX_WEB_UI_PORT = 6006;
    /**
     * Phoenix gRPC port.
     */
    protected static final int PHOENIX_GRPC_PORT = 4317;
    /**
     * Prometheus metrics port.
     */
    protected static final int PROMETHEUS_PORT = 9090;

    public ArconiaPhoenixContainer(DockerImageName dockerImageName, PhoenixDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), PHOENIX_WEB_UI_PORT);
        }
    }
}
