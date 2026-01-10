package io.arconia.dev.services.ollama;

import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * An {@link OllamaContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaOllamaContainer extends OllamaContainer {

    public ArconiaOllamaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

}
