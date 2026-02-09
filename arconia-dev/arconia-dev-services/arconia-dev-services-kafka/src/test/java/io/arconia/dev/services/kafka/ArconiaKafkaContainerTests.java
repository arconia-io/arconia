package io.arconia.dev.services.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaKafkaContainer}.
 */
class ArconiaKafkaContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaKafkaContainer(new KafkaDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new KafkaDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaKafkaContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaKafkaContainer.KAFKA_PORT));
    }

}
