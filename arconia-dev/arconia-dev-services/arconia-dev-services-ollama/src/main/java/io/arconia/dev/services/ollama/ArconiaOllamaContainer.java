package io.arconia.dev.services.ollama;

import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link OllamaContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaOllamaContainer extends OllamaContainer {

    private final OllamaDevServicesProperties properties;

    /**
     * Ollama HTTP API port.
     */
    private static final int OLLAMA_PORT = 11434;

    public ArconiaOllamaContainer(DockerImageName dockerImageName, OllamaDevServicesProperties properties) {
        super(dockerImageName);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), OLLAMA_PORT);
        }
    }
}
