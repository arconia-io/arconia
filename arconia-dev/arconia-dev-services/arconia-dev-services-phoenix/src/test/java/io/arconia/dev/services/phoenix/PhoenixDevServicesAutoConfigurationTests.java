package io.arconia.dev.services.phoenix;

import java.util.List;
import java.util.Map;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PhoenixDevServicesAutoConfiguration}.
 */
class PhoenixDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    PhoenixDevServicesAutoConfigurationTests() {
        super(PhoenixDevServicesAutoConfiguration.class, "phoenix", PhoenixContainer.class, OtlpTracingConnectionDetails.class);
    }

    @Override
    protected boolean isSkippedInTestsByDefault() {
        return true;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new PhoenixDevServicesProperties();
        return asDiscoverableContainer(PhoenixDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected Map<String, Integer> additionalFixedPortProperties() {
        return Map.of("otlp-grpc-port", PhoenixContainer.GRPC_PORT);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        PhoenixContainer container = (PhoenixContainer) discoveredContainer;
        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF))
                .isEqualTo("http://%s:%d".formatted(container.getHost(), container.getHttpPort()) + OtlpTracingConnectionDetails.TRACES_PATH);
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC))
                .isEqualTo("http://%s:%d".formatted(container.getHost(), container.getGrpcPort()));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PhoenixContainer.class));
    }

    @Test
    @DisabledIfDockerUnavailable
    void devServiceLinksExposePhoenixUi() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).getLinks();
            assertThat(links).singleElement().satisfies(link -> {
                assertThat(link.label()).isEqualTo("Phoenix UI");
                assertThat(link.url()).startsWith("http://")
                        .endsWith(":" + container.getMappedPort(PhoenixContainer.HTTP_PORT));
            });
        });
    }

    @Test
    void logsAndMetricsNotExportedWhileActive() {
        contextRunnerWithOpenTelemetryExporters()
                .withPropertyValues(
                        "arconia.otel.logs.exporter.type=otlp",
                        "arconia.otel.metrics.exporter.type=otlp"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
                    assertThat(context).doesNotHaveBean(OtlpHttpMetricExporter.class);
                });
    }

    @Test
    void logsAndMetricsExportedWhenDisabled() {
        contextRunnerWithOpenTelemetryExporters()
                .withPropertyValues("arconia.dev.services.phoenix.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                    assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                });
    }

    /**
     * The context runner with the OpenTelemetry exporter auto-configurations, which decide
     * from the exporter type properties whether to create the OTLP exporters.
     */
    private ApplicationContextRunner contextRunnerWithOpenTelemetryExporters() {
        return getContextRunner()
                .withConfiguration(AutoConfigurations.of(OpenTelemetryExporterAutoConfiguration.class,
                        OpenTelemetryLoggingExporterAutoConfiguration.class, OpenTelemetryMetricsExporterAutoConfiguration.class))
                .withBean(CardinalityLimitSelector.class, CardinalityLimitSelector::defaultCardinalityLimitSelector)
                .withBean(OpenTelemetryMetricsProperties.class, OpenTelemetryMetricsProperties::new);
    }

}
