package io.arconia.dev.services.valkey;

import org.testcontainers.utility.DockerImageName;

import io.arconia.testcontainers.valkey.ValkeyContainer;

/**
 * A {@link ValkeyContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaValkeyContainer extends ValkeyContainer {

    private final ValkeyDevServicesProperties properties;

    /**
     * Redis-compatible RESP protocol port.
     */
    private static final int VALKEY_PORT = 6379;

    public ArconiaValkeyContainer(DockerImageName dockerImageName, ValkeyDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), VALKEY_PORT);
        }
    }
}
