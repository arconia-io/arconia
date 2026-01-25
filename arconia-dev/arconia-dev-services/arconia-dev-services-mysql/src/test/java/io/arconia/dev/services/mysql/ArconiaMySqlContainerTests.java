package io.arconia.dev.services.mysql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaMySqlContainer}.
 */
class ArconiaMySqlContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaMySqlContainer(new MySqlDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new MySqlDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaMySqlContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaMySqlContainer.MYSQL_PORT));
    }

}
