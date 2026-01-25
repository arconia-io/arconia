package io.arconia.dev.services.pulsar;

import org.junit.jupiter.api.Test;
import org.testcontainers.pulsar.PulsarContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaPulsarContainer}.
 */
class ArconiaPulsarContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaPulsarContainer(new PulsarDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new PulsarDevServicesProperties();
        properties.setPort(1234);
        properties.setManagementConsolePort(5678);

        var container = new ArconiaPulsarContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + PulsarContainer.BROKER_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getManagementConsolePort() + ":" + PulsarContainer.BROKER_HTTP_PORT));
    }

}
