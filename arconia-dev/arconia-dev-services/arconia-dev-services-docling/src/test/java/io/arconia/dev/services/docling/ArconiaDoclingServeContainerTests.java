package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaDoclingServeContainer}.
 */
class ArconiaDoclingServeContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaDoclingServeContainer(new DoclingDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new DoclingDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaDoclingServeContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + DoclingServeContainer.DEFAULT_DOCLING_PORT));
    }

}
