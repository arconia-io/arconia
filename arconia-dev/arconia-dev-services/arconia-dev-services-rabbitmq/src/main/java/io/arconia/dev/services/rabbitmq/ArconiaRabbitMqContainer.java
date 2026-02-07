package io.arconia.dev.services.rabbitmq;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link RabbitMQContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaRabbitMqContainer extends RabbitMQContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "rabbitmq";

    private static final Logger logger = LoggerFactory.getLogger(ArconiaRabbitMqContainer.class);

    private final RabbitMqDevServicesProperties properties;

    static final int AMQP_PORT = 5672;

    static final int HTTP_PORT = 15672;

    public ArconiaRabbitMqContainer(RabbitMqDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), AMQP_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getManagementConsolePort())) {
            addFixedExposedPort(properties.getManagementConsolePort(), HTTP_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("RabbitMQ Management Console: {}", getHttpUrl());
    }

}
