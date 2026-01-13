package io.arconia.dev.services.rabbitmq;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link RabbitMQContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaRabbitMqContainer extends RabbitMQContainer {

    private final RabbitMqDevServicesProperties properties;

    private static final Logger logger = LoggerFactory.getLogger(ArconiaRabbitMqContainer.class);

    /**
     * Management web UI port.
     */
    private static final int RABBITMQ_MANAGEMENT_PORT = 15672;

    /**
     * AMQP messaging protocol port.
     */
    private static final int RABBITMQ_PORT = 5672;

    public ArconiaRabbitMqContainer(DockerImageName dockerImageName, RabbitMqDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), RABBITMQ_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("RabbitMQ Management Console: {}", getHttpUrl());
    }

}
