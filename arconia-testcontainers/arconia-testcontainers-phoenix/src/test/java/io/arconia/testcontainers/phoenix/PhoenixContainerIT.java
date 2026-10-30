package io.arconia.testcontainers.phoenix;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
            .parse("arizephoenix/phoenix:version-20.27-nonroot");

    @Test
    void containerStartsAndStopsSuccessfully() throws Exception {
        try (var container = new PhoenixContainer(IMAGE)) {
            container.start();
            assertThat(container.getCurrentContainerInfo().getState().getStatus())
                    .isEqualTo("running");
            assertThat(container.getEnvMap()).containsEntry("PHOENIX_TELEMETRY_ENABLED", "false");

            var request = HttpRequest.newBuilder(URI.create(container.getPhoenixUrl() + "/readyz")).GET().build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);

            container.stop();
        }
    }

}
