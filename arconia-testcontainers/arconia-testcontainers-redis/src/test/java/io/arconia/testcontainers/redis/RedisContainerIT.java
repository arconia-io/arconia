package io.arconia.testcontainers.redis;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RedisContainer}.
 */
@EnabledIfDockerAvailable
class RedisContainerIT {

    private static final String IMAGE = "redis:8.10-alpine";

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
