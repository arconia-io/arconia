package io.arconia.dev.services.pulsar;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link PulsarContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaPulsarContainer extends PulsarContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "apachepulsar/pulsar";

    private static final Logger logger = LoggerFactory.getLogger(ArconiaPulsarContainer.class);

    private final PulsarDevServicesProperties properties;

    public ArconiaPulsarContainer(PulsarDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), BROKER_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getManagementConsolePort())) {
            addFixedExposedPort(properties.getManagementConsolePort(), BROKER_HTTP_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("Pulsar Management Console: {}", getHttpServiceUrl());
    }

}
