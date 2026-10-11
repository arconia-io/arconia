package io.arconia.dev.services.rabbitmq;

import java.util.Map;

import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaRabbitMqContainer}.
 */
class ArconiaRabbitMqContainerTests extends BaseDevServiceContainerTests<RabbitMqDevServicesProperties, ArconiaRabbitMqContainer> {

    @Override
    protected RabbitMqDevServicesProperties createProperties() {
        return new RabbitMqDevServicesProperties();
    }

    @Override
    protected ArconiaRabbitMqContainer createContainer(RabbitMqDevServicesProperties properties) {
        return new ArconiaRabbitMqContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(RabbitMqDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setManagementConsolePort(5678);
        return Map.of(
                1234, ArconiaRabbitMqContainer.AMQP_PORT,
                5678, ArconiaRabbitMqContainer.HTTP_PORT);
    }

}
