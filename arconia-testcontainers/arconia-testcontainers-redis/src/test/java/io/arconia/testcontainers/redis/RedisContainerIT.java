package io.arconia.testcontainers.redis;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RedisContainer}.
 */
@EnabledIfDockerAvailable
class RedisContainerIT {

    private static final DockerImageName IMAGE = DockerImageName
            .parse("redis:8.4-alpine");

    @Test
    void containerStartsAndStopsSuccessfully() {
        try (var container = new RedisContainer(IMAGE)) {
            container.start();
            assertThat(container.getCurrentContainerInfo().getState().getStatus())
                    .isEqualTo("running");
            container.stop();
        }
    }

}
