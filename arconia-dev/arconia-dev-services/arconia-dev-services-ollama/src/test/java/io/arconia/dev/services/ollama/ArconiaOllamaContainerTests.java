package io.arconia.dev.services.ollama;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaOllamaContainer}.
 */
class ArconiaOllamaContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaOllamaContainer(new OllamaDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new OllamaDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaOllamaContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + ArconiaOllamaContainer.OLLAMA_PORT));
    }

}
