package io.arconia.dev.services.phoenix;

import org.junit.jupiter.api.Test;

import io.arconia.testcontainers.phoenix.PhoenixContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaPhoenixContainer}.
 */
class ArconiaPhoenixContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaPhoenixContainer(new PhoenixDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new PhoenixDevServicesProperties();
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);

        var container = new ArconiaPhoenixContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + PhoenixContainer.HTTP_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getOtlpGrpcPort() + ":" + PhoenixContainer.GRPC_PORT));
    }

}
