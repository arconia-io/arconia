package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaOracleXeContainer}.
 */
class ArconiaOracleXeContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaOracleXeContainer(new OracleXeDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new OracleXeDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaOracleXeContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaOracleXeContainer.ORACLE_PORT));
    }

}
