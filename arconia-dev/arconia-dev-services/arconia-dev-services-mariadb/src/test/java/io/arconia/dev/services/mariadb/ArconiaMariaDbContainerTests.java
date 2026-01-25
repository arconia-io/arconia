package io.arconia.dev.services.mariadb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaMariaDbContainer}.
 */
class ArconiaMariaDbContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaMariaDbContainer(new MariaDbDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new MariaDbDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaMariaDbContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaMariaDbContainer.MARIADB_PORT));
    }

}
