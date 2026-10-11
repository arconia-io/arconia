package io.arconia.dev.services.docling;

import java.util.Map;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaDoclingServeContainer}.
 */
class ArconiaDoclingServeContainerTests extends BaseDevServiceContainerTests<DoclingDevServicesProperties, ArconiaDoclingServeContainer> {

    @Override
    protected DoclingDevServicesProperties createProperties() {
        return new DoclingDevServicesProperties();
    }

    @Override
    protected ArconiaDoclingServeContainer createContainer(DoclingDevServicesProperties properties) {
        return new ArconiaDoclingServeContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(DoclingDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, DoclingServeContainer.DEFAULT_DOCLING_PORT);
    }

    @Test
    void uiEnabledByDefault() {
        var container = new ArconiaDoclingServeContainer(new DoclingDevServicesProperties());
        configure(container);

        assertThat(container.getEnvMap()).containsEntry("DOCLING_SERVE_ENABLE_UI", "true");
    }

}
