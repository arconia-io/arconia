package io.arconia.dev.services.rabbitmq;

import java.util.List;

import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link RabbitMQContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaRabbitMqContainer extends RabbitMQContainer implements DevServiceLinkProvider {

    private final RabbitMqDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "rabbitmq";

    static final int AMQP_PORT = 5672;

    static final int HTTP_PORT = 15672;

    public ArconiaRabbitMqContainer(RabbitMqDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);

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

    @Override
    public List<DevServiceLink> devServiceLinks() {
        return List.of(DevServiceLink.builder()
                .id("rabbitmq")
                .label("RabbitMQ Management Console")
                .url(getHttpUrl())
                .build());
    }

}
