package io.arconia.dev.services.ollama;

import java.io.IOException;
import java.util.List;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link OllamaContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaOllamaContainer extends OllamaContainer {

    private static final Logger logger = LoggerFactory.getLogger(ArconiaOllamaContainer.class);

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
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), OLLAMA_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        properties.getModels().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .forEach(this::pullModel);
    }

    private void pullModel(String model) {
        logger.info("Pulling Ollama model: {}. The first pull can take a while.", model);
        try {
            ExecResult result = execInContainer("ollama", "pull", model);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Error pulling Ollama model '%s': %s"
                        .formatted(model, result.getStderr()));
            }
            logger.info("Pulled Ollama model: {}", model);
        } catch (IOException ex) {
            throw new IllegalStateException("Error pulling Ollama model: %s".formatted(model), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while pulling Ollama model: %s".formatted(model), ex);
        }
    }

}
