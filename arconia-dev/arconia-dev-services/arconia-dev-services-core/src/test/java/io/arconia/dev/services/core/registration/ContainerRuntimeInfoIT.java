package io.arconia.dev.services.core.registration;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests for {@link ContainerRuntimeInfo}.
 */
@DisabledIfDockerUnavailable
class ContainerRuntimeInfoIT {

    private static final DockerImageName NGINX_IMAGE = DockerImageName.parse("nginx:alpine3.24");

    @Test
    void copyFileFromContainerReturnsTheFileContent() {
        try (GenericContainer<?> container = new GenericContainer<>(NGINX_IMAGE)) {
            container.start();

            byte[] content = ContainerRuntimeInfo.copyFileFromContainer(container.getContainerId(), "/etc/nginx/nginx.conf");

            assertThat(new String(content, StandardCharsets.UTF_8)).contains("http {");
        }
    }

    @Test
    void whenFileDoesNotExistThenThrowNamingIt() {
        try (GenericContainer<?> container = new GenericContainer<>(NGINX_IMAGE)) {
            container.start();
            String containerId = container.getContainerId();

            assertThatIllegalStateException()
                    .isThrownBy(() -> ContainerRuntimeInfo.copyFileFromContainer(containerId, "/missing.txt"))
                    .withMessageContaining("Failed to copy the file /missing.txt from the container");
        }
    }

}
