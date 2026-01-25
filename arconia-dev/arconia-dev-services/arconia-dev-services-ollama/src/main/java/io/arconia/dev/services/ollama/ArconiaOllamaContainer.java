package io.arconia.dev.services.ollama;

import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link OllamaContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaOllamaContainer extends OllamaContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "ollama/ollama";

    private final OllamaDevServicesProperties properties;

    static final int OLLAMA_PORT = 11434;

    public ArconiaOllamaContainer(OllamaDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), OLLAMA_PORT);
        }
    }

}
