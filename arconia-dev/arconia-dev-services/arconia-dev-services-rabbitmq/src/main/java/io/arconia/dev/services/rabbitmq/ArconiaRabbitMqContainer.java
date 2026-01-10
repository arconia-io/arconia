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

    private static final Logger logger = LoggerFactory.getLogger(ArconiaRabbitMqContainer.class);

    public ArconiaRabbitMqContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("RabbitMQ Management Console: {}", getHttpUrl());
    }

}
