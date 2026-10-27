package io.arconia.testcontainers.openlit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.github.dockerjava.api.model.Container;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenLitContainer}.
 */
@EnabledIfDockerAvailable
class OpenLitContainerIT {

    private static final DockerImageName IMAGE = DockerImageName.parse("ghcr.io/openlit/openlit:2.1.1");
    private static final DockerImageName CLICKHOUSE_IMAGE = DockerImageName.parse("clickhouse/clickhouse-server:26.10-distroless");

    // Minimal OTLP/JSON payload with a single span.
    private static final String TRACES_PAYLOAD = """
            {
              "resourceSpans": [{
                "resource": { "attributes": [{ "key": "service.name", "value": { "stringValue": "openlit-it" } }] },
                "scopeSpans": [{
                  "scope": { "name": "openlit-it" },
                  "spans": [{
                    "traceId": "5b8efff798038103d269b633813fc60c",
                    "spanId": "eee19b7ec3c1b174",
                    "name": "test-span",
                    "kind": 1,
                    "startTimeUnixNano": "1700000000000000000",
                    "endTimeUnixNano": "1700000001000000000"
                  }]
                }]
              }]
            }
            """;

    @Test
    void containerStartsAndAcceptsOtlpData() throws Exception {
        try (var container = new OpenLitContainer(IMAGE).withClickHouseImage(CLICKHOUSE_IMAGE)
                .withEnv("DEMO_ACCOUNT_EMAIL", "")) {
            container.start();
            assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
            assertThat(container.getOtlpGrpcUrl()).startsWith("http://");
            assertThat(container.getOtlpHttpUrl()).startsWith("http://");
            assertThat(container.getOpenLitUrl()).startsWith("http://");

            // User-provided environment variables take precedence over the container defaults.
            assertThat(container.getEnvMap())
                    .containsEntry("DEMO_ACCOUNT_EMAIL", "")
                    .containsEntry("DEMO_ACCOUNT_PASSWORD", "openlituser")
                    .containsEntry("TELEMETRY_ENABLED", "false");

            var request = HttpRequest.newBuilder(URI.create(container.getOtlpHttpUrl() + "/v1/traces"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(TRACES_PAYLOAD))
                    .build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);

            String networkId = container.getNetwork().getId();
            List<String> containerIds = containersOnNetwork(networkId);
            assertThat(containerIds).hasSize(2);

            container.stop();
            assertThat(existingContainers(containerIds)).isEmpty();
            assertThat(networkExists(networkId)).isFalse();
        }
    }

    @Test
    void containerKeepsProvidedNetworkWhenStopped() {
        try (var network = Network.newNetwork();
                var container = new OpenLitContainer(IMAGE).withClickHouseImage(CLICKHOUSE_IMAGE).withNetwork(network)) {
            container.start();
            assertThat(container.getClickHouseLogs()).isNotEmpty();

            container.stop();
            assertThat(networkExists(network.getId())).isTrue();
        }
    }

    private static List<String> containersOnNetwork(String networkId) {
        return DockerClientFactory.instance().client().listContainersCmd()
                .withNetworkFilter(List.of(networkId))
                .exec()
                .stream()
                .map(Container::getId)
                .toList();
    }

    private static List<Container> existingContainers(List<String> containerIds) {
        return DockerClientFactory.instance().client().listContainersCmd()
                .withShowAll(true)
                .withIdFilter(containerIds)
                .exec();
    }

    private static boolean networkExists(String networkId) {
        return !DockerClientFactory.instance().client().listNetworksCmd()
                .withIdFilter(networkId)
                .exec()
                .isEmpty();
    }

}
