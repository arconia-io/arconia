package io.arconia.dev.services.lgtm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeGrafanaAndOtlpUrls() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).links();
            assertThat(links).extracting(DevServiceLink::label)
                    .containsExactly("Grafana", "OTLP/HTTP", "OTLP/gRPC");
            assertThat(links).allSatisfy(link -> assertThat(link.url()).startsWith("http://" + container.getHost() + ":"));
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
                        .containsAnyOf("JVM Metrics (Micrometer)", "JVM Metrics (OpenTelemetry)")
                        .contains("Spring Boot Test Dashboard")
                        .doesNotContain("JVM Overview (OpenTelemetry)")
                        .doesNotContain("RED Metrics");
            }
        });
    }

}
