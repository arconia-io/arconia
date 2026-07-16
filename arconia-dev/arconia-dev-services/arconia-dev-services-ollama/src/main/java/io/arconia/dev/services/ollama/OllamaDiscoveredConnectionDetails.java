package io.arconia.dev.services.ollama;

import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link OllamaConnectionDetails} for connecting to a shared Ollama dev service
 * running in a container discovered from another application.
 */
final class OllamaDiscoveredConnectionDetails implements OllamaConnectionDetails {

    private final String baseUrl;

    OllamaDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.baseUrl = "http://%s:%d".formatted(container.host(), container.mappedPort(ArconiaOllamaContainer.OLLAMA_PORT));
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

}
