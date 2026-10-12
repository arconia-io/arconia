package io.arconia.dev.services.lgtm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.grafana.LgtmStackContainer;

import io.arconia.dev.services.api.config.ResourceMapping;
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
        return asDiscoverableContainer(LgtmDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
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

    @Nested
    class SpringBootDashboards {

        private static final String DASHBOARD_CONTAINER_PATH = LgtmDevServicesAutoConfiguration.SPRING_BOOT_DASHBOARDS_PATH + "/spring-boot.json";

        private static final String[] DASHBOARD_PROPERTIES = {
                "arconia.dev.services.lgtm.resources[0].source-path=classpath:grafana/spring-boot.json",
                "arconia.dev.services.lgtm.resources[0].container-path=" + DASHBOARD_CONTAINER_PATH
        };

        @Test
        void whenSpringBootDashboardIsNotConfigured() {
            var properties = new LgtmDevServicesProperties();

            assertThat(LgtmDevServicesAutoConfiguration.hasSpringBootDashboardResources(properties)).isFalse();
        }

        @Test
        void whenSpringBootDashboardIsConfigured() {
            var properties = new LgtmDevServicesProperties();
            properties.setResources(List.of(new ResourceMapping("classpath:grafana/spring-boot.json", DASHBOARD_CONTAINER_PATH)));

            assertThat(LgtmDevServicesAutoConfiguration.hasSpringBootDashboardResources(properties)).isTrue();
        }

        @Test
        void whenSpringBootDashboardIsConfiguredThenProviderIsCorrect() {
            assertThat(LgtmDevServicesAutoConfiguration.springBootDashboardsProvider())
                    .contains("apiVersion: 1")
                    .contains("name: Spring Boot")
                    .contains("type: file")
                    .contains("path: " + LgtmDevServicesAutoConfiguration.SPRING_BOOT_DASHBOARDS_PATH)
                    .contains("foldersFromFilesStructure: false");
        }

        @Test
        void springBootDashboardResourceIsConfigured() {
            getContextRunner()
                    .withPropertyValues(DASHBOARD_PROPERTIES)
                    .run(context -> {
                        var container = context.getBean(getContainerClass());
                        assertThat(container.getCopyToFileContainerPathMap()).containsValue(DASHBOARD_CONTAINER_PATH);
                    });
        }

        @Test
        @DisabledIfDockerUnavailable
        void springBootDashboardIsLoaded() {
            contextRunnerWithContainerLifecycle()
                    .withPropertyValues(DASHBOARD_PROPERTIES)
                    .run(context -> {
                        var container = context.getBean(getContainerClass());
                        var grafanaUrl = "http://%s:%d".formatted(container.getHost(),
                                container.getMappedPort(LgtmDevServicesAutoConfiguration.GRAFANA_PORT));
                        var request = HttpRequest.newBuilder().uri(URI.create(grafanaUrl + "/api/search")).GET().build();

                        try (var client = HttpClient.newHttpClient()) {
                            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                            assertThat(response.statusCode()).isEqualTo(200);
                            assertThat(response.body()).contains("Spring Boot Test Dashboard");
                        }
                    });
        }

    }

}
