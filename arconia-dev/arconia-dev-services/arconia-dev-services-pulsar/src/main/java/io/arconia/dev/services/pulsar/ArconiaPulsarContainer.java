package io.arconia.dev.services.pulsar;

import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link PulsarContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaPulsarContainer extends PulsarContainer {

    private final PulsarDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "apachepulsar/pulsar";

    public ArconiaPulsarContainer(PulsarDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), BROKER_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getAdminPort())) {
            addFixedExposedPort(properties.getAdminPort(), BROKER_HTTP_PORT);
        }
    }

}
