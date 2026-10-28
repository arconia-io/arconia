package io.arconia.testcontainers.langfuse;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.dockerjava.api.model.Container;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LangfuseContainer}.
 */
@EnabledIfDockerAvailable
class LangfuseContainerIT {

    private static final DockerImageName IMAGE = DockerImageName.parse("langfuse/langfuse:4.71");
    private static final String WORKER_IMAGE = "langfuse/langfuse-worker:4.71";
    private static final String POSTGRES_IMAGE = "postgres:18.6-alpine";
    private static final String CLICKHOUSE_IMAGE = "clickhouse/clickhouse-server:26.10-distroless";
    private static final String REDIS_IMAGE = "redis:8.10-alpine";
    private static final String FLOCI_IMAGE = "floci/floci:2.3.0";

    private static final String TRACE_ID = "5b8efff798038103d269b633813fc60c";

    // Minimal OTLP/JSON payload with a single span.
    private static final String TRACES_PAYLOAD = """
            {
              "resourceSpans": [{
                "resource": { "attributes": [{ "key": "service.name", "value": { "stringValue": "langfuse-it" } }] },
                "scopeSpans": [{
                  "scope": { "name": "langfuse-it" },
                  "spans": [{
                    "traceId": "%s",
                    "spanId": "eee19b7ec3c1b174",
                    "name": "test-span",
                    "kind": 1,
                    "startTimeUnixNano": "%d",
                    "endTimeUnixNano": "%d"
                  }]
                }]
              }]
            }
            """;

    @Test
    void containerStartsAndIngestsOtlpTraces() throws Exception {
        try (var container = new LangfuseContainer(IMAGE)
                .withWorkerImage(WORKER_IMAGE)
                .withPostgresImage(POSTGRES_IMAGE)
                .withClickHouseImage(CLICKHOUSE_IMAGE)
                .withRedisImage(REDIS_IMAGE)
                .withFlociImage(FLOCI_IMAGE)) {
            container.start();
            assertThat(container.getEnvMap()).containsEntry("TELEMETRY_ENABLED", "false");

            var httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
            long now = Instant.now().toEpochMilli() * 1_000_000;
            var exportRequest = HttpRequest.newBuilder(URI.create(container.getOtlpTracesUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", container.getBasicAuthHeader())
                    .header("x-langfuse-ingestion-version", "4")
                    .POST(HttpRequest.BodyPublishers.ofString(TRACES_PAYLOAD.formatted(TRACE_ID, now, now + 1_000_000_000L)))
                    .build();
            // The default JDK client requests an upgrade to HTTP/2, as the OpenTelemetry Java exporters do.
            var exportResponse = HttpClient.newHttpClient().send(exportRequest, HttpResponse.BodyHandlers.ofString());
            assertThat(exportResponse.statusCode()).as(exportResponse.body()).isBetween(200, 299);

            // The worker processes the ingested span asynchronously.
            var observationsRequest = HttpRequest.newBuilder(URI.create(container.getLangfuseUrl() + "/api/public/v2/observations?traceId=" + TRACE_ID))
                    .header("Authorization", container.getBasicAuthHeader())
                    .GET()
                    .build();
            HttpResponse<String> observationsResponse = null;
            Instant deadline = Instant.now().plus(Duration.ofMinutes(2));
            while (Instant.now().isBefore(deadline)) {
                observationsResponse = httpClient.send(observationsRequest, HttpResponse.BodyHandlers.ofString());
                if (observationsResponse.statusCode() == 200 && observationsResponse.body().contains("test-span")) {
                    break;
                }
                Thread.sleep(2000);
            }
            assertThat(observationsResponse).isNotNull();
            assertThat(observationsResponse.statusCode()).as(observationsResponse.body()).isEqualTo(200);
            assertThat(observationsResponse.body()).contains("test-span");

            // The initial user can sign in to the UI, also with a random port, since the sign-in page
            // doesn't follow the redirect to the NEXTAUTH_URL.
            var browser = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .cookieHandler(new CookieManager())
                    .build();
            var csrfResponse = browser.send(HttpRequest.newBuilder(URI.create(container.getLangfuseUrl() + "/api/auth/csrf")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            Matcher csrfToken = Pattern.compile("\"csrfToken\"\\s*:\\s*\"([^\"]+)\"").matcher(csrfResponse.body());
            assertThat(csrfToken.find()).as(csrfResponse.body()).isTrue();
            String form = "csrfToken=%s&email=%s&password=%s&json=true".formatted(csrfToken.group(1),
                    URLEncoder.encode(LangfuseContainer.DEFAULT_USER_EMAIL, StandardCharsets.UTF_8),
                    URLEncoder.encode(LangfuseContainer.DEFAULT_USER_PASSWORD, StandardCharsets.UTF_8));
            var signInResponse = browser.send(HttpRequest.newBuilder(URI.create(container.getLangfuseUrl() + "/api/auth/callback/credentials"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(signInResponse.statusCode()).as(signInResponse.body()).isEqualTo(200);
            var sessionResponse = browser.send(HttpRequest.newBuilder(URI.create(container.getLangfuseUrl() + "/api/auth/session")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(sessionResponse.body()).contains(LangfuseContainer.DEFAULT_USER_EMAIL);

            // The containers Langfuse needs are removed together with it.
            String networkId = container.getNetwork().getId();
            List<String> containerIds = DockerClientFactory.instance().client().listContainersCmd()
                    .withNetworkFilter(List.of(networkId))
                    .exec()
                    .stream()
                    .map(Container::getId)
                    .toList();
            assertThat(containerIds).hasSize(6);

            container.stop();
            assertThat(DockerClientFactory.instance().client().listContainersCmd()
                    .withShowAll(true)
                    .withIdFilter(containerIds)
                    .exec()).isEmpty();
        }
    }

}
