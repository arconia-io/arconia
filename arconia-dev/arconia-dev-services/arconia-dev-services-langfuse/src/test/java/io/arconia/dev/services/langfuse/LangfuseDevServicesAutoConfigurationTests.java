package io.arconia.dev.services.langfuse;

import java.util.List;

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
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.langfuse.LangfuseContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LangfuseDevServicesAutoConfiguration}.
 */
class LangfuseDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    LangfuseDevServicesAutoConfigurationTests() {
        super(LangfuseDevServicesAutoConfiguration.class, "langfuse", LangfuseContainer.class, OtlpTracingConnectionDetails.class);
    }

    @Override
    protected boolean isSkippedInTestsByDefault() {
        return true;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new LangfuseDevServicesProperties();
        return asDiscoverableContainer(LangfuseDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        // Langfuse accepts traces only.
        assertThat(context).doesNotHaveBean(OtlpMetricsConnectionDetails.class);
        assertThat(context).doesNotHaveBean(OtlpLoggingConnectionDetails.class);
        LangfuseContainer container = (LangfuseContainer) discoveredContainer;
        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).isEqualTo(container.getOtlpTracesUrl());
        assertThat(connectionDetails.getHeaders())
                .containsEntry("Authorization", container.getBasicAuthHeader())
                .containsEntry("x-langfuse-ingestion-version", "4");
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LangfuseContainer.class));
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
                .withPropertyValues("arconia.dev.services.langfuse.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                    assertThat(context).hasSingleBean(OtlpHttpMetricExporter.class);
                });
    }

    @Test
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeLangfuseUi() {
        contextRunnerWithContainerLifecycle().run(context -> {
            var container = context.getBean(getContainerClass());
            List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).getLinks();
            assertThat(links).singleElement().satisfies(link -> {
                assertThat(link.label()).isEqualTo("Langfuse UI");
                assertThat(link.url()).startsWith("http://" + container.getHost() + ":");
            });
        });
    }

    private ApplicationContextRunner contextRunnerWithOpenTelemetryExporters() {
        return getContextRunner()
                .withConfiguration(AutoConfigurations.of(OpenTelemetryExporterAutoConfiguration.class,
                        OpenTelemetryLoggingExporterAutoConfiguration.class, OpenTelemetryMetricsExporterAutoConfiguration.class))
                .withBean(CardinalityLimitSelector.class, CardinalityLimitSelector::defaultCardinalityLimitSelector)
                .withBean(OpenTelemetryMetricsProperties.class, OpenTelemetryMetricsProperties::new);
    }

}
