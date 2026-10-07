package io.arconia.dev.services.rabbitmq;

import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link RabbitMQContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaRabbitMqContainer extends RabbitMQContainer {

    private final RabbitMqDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "rabbitmq";

    static final int AMQP_PORT = 5672;

    static final int HTTP_PORT = 15672;

    public ArconiaRabbitMqContainer(RabbitMqDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;


        this.withAdminUser(properties.getUsername());
        this.withAdminPassword(properties.getPassword());
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), AMQP_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getManagementConsolePort())) {
            addFixedExposedPort(properties.getManagementConsolePort(), HTTP_PORT);
        }
    }

}
