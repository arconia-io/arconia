package io.arconia.dev.services.rabbitmq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaRabbitMqContainer}.
 */
class ArconiaRabbitMqContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaRabbitMqContainer(new RabbitMqDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new RabbitMqDevServicesProperties();
        properties.setPort(1234);
        properties.setManagementConsolePort(5678);

        var container = new ArconiaRabbitMqContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaRabbitMqContainer.AMQP_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getManagementConsolePort() + ":" + ArconiaRabbitMqContainer.HTTP_PORT));
    }

}
