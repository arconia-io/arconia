package io.arconia.dev.services.pulsar;

import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link PulsarContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaPulsarContainer extends PulsarContainer {

    private final PulsarDevServicesProperties properties;

    /**
     * Pulsar admin REST API port.
     */
    private static final int PULSAR_ADMIN_PORT = 8080;

    /**
     * Pulsar binary protocol port.
     */
    private static final int PULSAR_PORT = 6650;

    public ArconiaPulsarContainer(DockerImageName dockerImageName, PulsarDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), PULSAR_PORT);
        }
    }
}
