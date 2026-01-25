package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaOracleContainer}.
 */
class ArconiaOracleContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaOracleContainer(new OracleDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new OracleDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaOracleContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaOracleContainer.ORACLE_PORT));
    }

}
