package io.arconia.dev.services.lgtm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LgtmDevServicesAutoConfiguration}.
 */
class LgtmDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    LgtmDevServicesAutoConfigurationTests() {
        super(LgtmDevServicesAutoConfiguration.class, "lgtm", LgtmStackContainer.class, OtlpTracingConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new LgtmDevServicesProperties();
        var dashboards = LgtmDashboards.collect(new MockEnvironment(), new DefaultResourceLoader(), properties.getDashboards());
        return asDiscoverableContainer(LgtmDevServicesAutoConfiguration.createContainer(properties, dashboards), properties, ownerId);
    }

    @Override
    protected Map<String, Integer> additionalFixedPortProperties() {
        return Map.of(
                "otlp-grpc-port", LgtmDevServicesAutoConfiguration.OTLP_GRPC_PORT,
                "otlp-http-port", LgtmDevServicesAutoConfiguration.OTLP_HTTP_PORT,
                "loki-port", LgtmDevServicesAutoConfiguration.LOKI_PORT,
                "tempo-port", LgtmDevServicesAutoConfiguration.TEMPO_PORT,
                "prometheus-port", LgtmDevServicesAutoConfiguration.PROMETHEUS_PORT);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
        assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
        LgtmStackContainer container = (LgtmStackContainer) discoveredContainer;
        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF))
                .isEqualTo(container.getOtlpHttpUrl() + OtlpTracingConnectionDetails.TRACES_PATH);
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC)).isEqualTo(container.getOtlpGrpcUrl());
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LgtmStackContainer.class));
    }

    @Test
    void tempoMcpServerEnabledInContainer() {
        var properties = new LgtmDevServicesProperties();
        var dashboards = LgtmDashboards.collect(new MockEnvironment(), new DefaultResourceLoader(), properties.getDashboards());
        var container = LgtmDevServicesAutoConfiguration.createContainer(properties, dashboards);
        assertThat(container.getEnvMap())
                .containsEntry(LgtmDevServicesAutoConfiguration.TEMPO_EXTRA_ARGS_ENV, LgtmDevServicesAutoConfiguration.TEMPO_MCP_SERVER_ARG);
    }

    @Test
    void grafanaAdminCredentialsConfiguredInContainer() {
        var properties = new LgtmDevServicesProperties();
        var dashboards = LgtmDashboards.collect(new MockEnvironment(), new DefaultResourceLoader(), properties.getDashboards());
        var container = LgtmDevServicesAutoConfiguration.createContainer(properties, dashboards);
        assertThat(container.getEnvMap())
                .containsEntry("GF_SECURITY_ADMIN_USER", LgtmDevServicesAutoConfiguration.GRAFANA_ADMIN_USER)
                .containsEntry("GF_SECURITY_ADMIN_PASSWORD", LgtmDevServicesAutoConfiguration.GRAFANA_ADMIN_PASSWORD);
    }

    @Test
    @DisabledIfDockerUnavailable
    void grafanaAdminSignsInWithArconiaCredentials() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(LgtmStackContainer.class);
            var credentials = Base64.getEncoder().encodeToString("%s:%s".formatted(
                    LgtmDevServicesAutoConfiguration.GRAFANA_ADMIN_USER, LgtmDevServicesAutoConfiguration.GRAFANA_ADMIN_PASSWORD)
                    .getBytes(StandardCharsets.UTF_8));
            var request = HttpRequest.newBuilder().uri(URI.create(container.getGrafanaHttpUrl() + "/api/user"))
                    .header("Authorization", "Basic " + credentials)
                    .GET().build();

            try (var client = HttpClient.newHttpClient()) {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("\"login\":\"arconia\"");
            }
        });
    }

    @Test
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeGrafanaOtlpPrometheusAndTempoMcpUrls() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).links();
            assertThat(links).extracting(DevServiceLink::label)
                    .containsExactly("Grafana", "OTLP/HTTP", "OTLP/gRPC", "Prometheus", "Tempo MCP");
            assertThat(links).allSatisfy(link -> assertThat(link.url()).startsWith("http://" + container.getHost() + ":"));
            assertThat(links).filteredOn(link -> link.label().equals("Tempo MCP"))
                    .singleElement().satisfies(link -> assertThat(link.url()).endsWith(LgtmDevServicesAutoConfiguration.TEMPO_MCP_PATH));
        });
    }

    @Test
    @DisabledIfDockerUnavailable
    void tempoMcpServerAnswersInitializeRequests() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            var mcpUrl = "http://%s:%d%s".formatted(container.getHost(),
                    container.getMappedPort(LgtmDevServicesAutoConfiguration.TEMPO_PORT), LgtmDevServicesAutoConfiguration.TEMPO_MCP_PATH);
            var initialize = """
                    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"arconia-tests","version":"1"}}}
                    """;
            var request = HttpRequest.newBuilder().uri(URI.create(mcpUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(initialize))
                    .build();

            try (var client = HttpClient.newHttpClient()) {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).contains("serverInfo");
            }
        });
    }

    @Test
    @DisabledIfDockerUnavailable
    void grafanaDashboardsReplaceTheBundledOnes() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            var grafanaUrl = "http://%s:%d".formatted(container.getHost(),
                    container.getMappedPort(LgtmDevServicesAutoConfiguration.GRAFANA_PORT));
            var request = HttpRequest.newBuilder().uri(URI.create(grafanaUrl + "/api/search")).GET().build();

            try (var client = HttpClient.newHttpClient()) {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body())
                        .containsAnyOf("arconia-jvm-micrometer", "arconia-jvm-opentelemetry")
                        .contains("arconia-logging")
                        .contains("Spring Boot Test Dashboard")
                        .doesNotContain("JVM Overview (OpenTelemetry)")
                        .doesNotContain("RED Metrics");
            }
        });
    }

}
