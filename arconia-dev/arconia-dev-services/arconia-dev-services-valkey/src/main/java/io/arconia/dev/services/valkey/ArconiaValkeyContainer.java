package io.arconia.dev.services.valkey;

import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;
import io.arconia.testcontainers.valkey.ValkeyContainer;

/**
 * A {@link ValkeyContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaValkeyContainer extends ValkeyContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/valkey-io/valkey";

    private final ValkeyDevServicesProperties properties;

    public ArconiaValkeyContainer(ValkeyDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), VALKEY_PORT);
        }
    }

}
