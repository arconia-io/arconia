package io.arconia.testcontainers.phoenix;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PhoenixContainer}.
 */
@EnabledIfDockerAvailable
class PhoenixContainerIT {

    private static final DockerImageName IMAGE = DockerImageName
            .parse("arizephoenix/phoenix:version-12.31-nonroot");

    @Test
    void containerStartsAndStopsSuccessfully() {
        try (var container = new PhoenixContainer(IMAGE)) {
            container.start();
            assertThat(container.getCurrentContainerInfo().getState().getStatus())
                    .isEqualTo("running");
            container.stop();
        }
    }

}
