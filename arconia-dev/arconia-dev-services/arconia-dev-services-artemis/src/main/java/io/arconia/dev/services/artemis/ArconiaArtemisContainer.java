package io.arconia.dev.services.artemis;

import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link ArtemisContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaArtemisContainer extends ArtemisContainer {

    private final ArtemisDevServicesProperties properties;

    private static final int WEB_CONSOLE_PORT = 8161;

    public ArconiaArtemisContainer(DockerImageName dockerImageName, ArtemisDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), WEB_CONSOLE_PORT);
        }
    }
}
