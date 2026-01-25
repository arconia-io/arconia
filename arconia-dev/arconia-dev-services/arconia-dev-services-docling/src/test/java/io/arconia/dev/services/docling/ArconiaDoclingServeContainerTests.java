package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaDoclingServeContainer}.
 */
class ArconiaDoclingServeContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaDoclingServeContainer(generateConfig(), new DoclingDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new DoclingDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaDoclingServeContainer(generateConfig(), properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + DoclingServeContainer.DEFAULT_DOCLING_PORT));
    }

    private DoclingServeContainerConfig generateConfig() {
        return DoclingServeContainerConfig.builder()
                .image("docling")
                .build();
    }

}
