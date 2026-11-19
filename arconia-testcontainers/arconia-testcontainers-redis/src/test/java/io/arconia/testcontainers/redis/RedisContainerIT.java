package io.arconia.testcontainers.redis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
    void containerAcceptsCommandsFromTheHost() throws Exception {
        try (var container = new RedisContainer(IMAGE)) {
            container.start();

            // Redis answers right after the container starts, via the mapped port that clients use.
            try (var socket = new Socket(container.getHost(), container.getRedisPort());
                    var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                socket.getOutputStream().write("PING\r\n".getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                assertThat(reader.readLine()).isEqualTo("+PONG");
            }
        }
    }

}
