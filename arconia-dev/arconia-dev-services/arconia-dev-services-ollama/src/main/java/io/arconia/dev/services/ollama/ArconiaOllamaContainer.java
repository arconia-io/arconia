package io.arconia.dev.services.ollama;

import java.util.List;

import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link OllamaContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaOllamaContainer extends OllamaContainer {

    private final OllamaDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "ollama/ollama";

    static final int OLLAMA_PORT = 11434;

    public ArconiaOllamaContainer(OllamaDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));

        // Workaround for https://github.com/testcontainers/testcontainers-java/issues/9287
        // OllamaContainer assumes the nvidia runtime works if listed in docker info,
        // but Docker Desktop may inject a phantom nvidia runtime via WSL even without an NVIDIA GPU.
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            this.withCreateContainerCmdModifier(cmd -> {
                if (cmd.getHostConfig() != null) {
                    cmd.getHostConfig().withDeviceRequests(List.of());
                }
            });
        }

        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), OLLAMA_PORT);
        }
    }

}
