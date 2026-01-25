package io.arconia.testcontainers.valkey;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ValkeyContainer}.
 */
@EnabledIfDockerAvailable
class ValkeyContainerIT {

    private static final DockerImageName IMAGE = DockerImageName
            .parse("ghcr.io/valkey-io/valkey:9.0-alpine");

    @Test
    void containerStartsAndStopsSuccessfully() {
        try (var container = new ValkeyContainer(IMAGE)) {
            container.start();
            assertThat(container.getCurrentContainerInfo().getState().getStatus())
                    .isEqualTo("running");
            container.stop();
        }
    }

}
