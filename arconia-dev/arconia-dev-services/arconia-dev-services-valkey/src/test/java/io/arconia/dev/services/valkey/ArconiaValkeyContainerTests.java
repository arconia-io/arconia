package io.arconia.dev.services.valkey;

import org.junit.jupiter.api.Test;

import io.arconia.testcontainers.valkey.ValkeyContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaValkeyContainer}.
 */
class ArconiaValkeyContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaValkeyContainer(new ValkeyDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new ValkeyDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaValkeyContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ValkeyContainer.VALKEY_PORT));
    }

}
