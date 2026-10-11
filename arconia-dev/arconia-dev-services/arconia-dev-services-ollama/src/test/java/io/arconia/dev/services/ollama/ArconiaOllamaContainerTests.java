package io.arconia.dev.services.ollama;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaOllamaContainer}.
 */
class ArconiaOllamaContainerTests extends BaseDevServiceContainerTests<OllamaDevServicesProperties, ArconiaOllamaContainer> {

    @Override
    protected OllamaDevServicesProperties createProperties() {
        return new OllamaDevServicesProperties();
    }

    @Override
    protected ArconiaOllamaContainer createContainer(OllamaDevServicesProperties properties) {
        return new ArconiaOllamaContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(OllamaDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaOllamaContainer.OLLAMA_PORT);
    }

}
