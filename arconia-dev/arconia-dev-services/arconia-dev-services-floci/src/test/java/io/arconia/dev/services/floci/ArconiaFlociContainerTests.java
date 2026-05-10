package io.arconia.dev.services.floci;

import io.floci.testcontainers.FlociContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaFlociContainer}.
 */
@EnabledIfDockerAvailable
class ArconiaFlociContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaFlociContainer(new FlociDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    void whenExposedPortsAreConfigured() {
        var properties = new FlociDevServicesProperties();
        properties.setPort(14566);

        var container = new ArconiaFlociContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + FlociContainer.PORT));
    }

}
