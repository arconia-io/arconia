package io.arconia.dev.services.artemis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaArtemisContainer}.
 */
class ArconiaArtemisContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaArtemisContainer(new ArtemisDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new ArtemisDevServicesProperties();
        properties.setPort(1234);
        properties.setManagementConsolePort(5678);

        var container = new ArconiaArtemisContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaArtemisContainer.TCP_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getManagementConsolePort() + ":" + ArconiaArtemisContainer.WEB_CONSOLE_PORT));
    }

}
