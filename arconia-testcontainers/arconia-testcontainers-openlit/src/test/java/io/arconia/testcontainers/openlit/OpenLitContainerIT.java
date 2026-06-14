package io.arconia.testcontainers.openlit;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenLitContainer}.
 */
@EnabledIfDockerAvailable
class OpenLitContainerIT {

    private static final DockerImageName IMAGE = DockerImageName.parse("ghcr.io/openlit/openlit:1.22.0");
    private static final DockerImageName CLICKHOUSE_IMAGE = DockerImageName.parse("clickhouse/clickhouse-server:26.5-distroless");

    @Test
    void containerStartsAndStopsSuccessfully() {
        try (var container = new OpenLitContainer(IMAGE).withClickHouseImage(CLICKHOUSE_IMAGE)) {
            container.start();
            assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
            assertThat(container.getOtlpGrpcUrl()).startsWith("http://");
            assertThat(container.getOtlpHttpUrl()).startsWith("http://");
            assertThat(container.getOpenLitUrl()).startsWith("http://");
            container.stop();
        }
    }

}
