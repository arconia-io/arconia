package io.arconia.dev.services.lldap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaLldapContainer}.
 */
class ArconiaLldapContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaLldapContainer(new LldapDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new LldapDevServicesProperties();
        properties.setPort(1234);
        properties.setManagementConsolePort(5678);

        var container = new ArconiaLldapContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaLldapContainer.LDAP_PORT))
                .anyMatch(binding -> binding.startsWith(
                        properties.getManagementConsolePort() + ":" + ArconiaLldapContainer.UI_PORT));
    }

}
