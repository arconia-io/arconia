package io.arconia.dev.services.pulsar;

import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link PulsarContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPulsarContainer extends PulsarContainer {

    private final PulsarDevServicesProperties properties;

    /**
     * Pulsar Web UI port.
     */
    private static final int PULSAR_WEB_UI_PORT = 8080;

    /**
     * Pulsar binary protocol port.
     */
    protected static final int PULSAR_PORT = 6650;

        /**
     * Pulsar binary protocol port.
     */
    protected static final int PULSAR_TLS_PORT = 6651;

    public ArconiaPulsarContainer(DockerImageName dockerImageName, PulsarDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), PULSAR_WEB_UI_PORT);
        }
    }
}
