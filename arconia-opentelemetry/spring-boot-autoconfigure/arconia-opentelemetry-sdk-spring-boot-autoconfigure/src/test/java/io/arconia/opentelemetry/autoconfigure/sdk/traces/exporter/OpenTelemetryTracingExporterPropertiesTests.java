package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryTracingExporterProperties}.
 */
class OpenTelemetryTracingExporterPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.traces.exporter");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenTelemetryTracingExporterProperties properties = new OpenTelemetryTracingExporterProperties();

        assertThat(properties.getType()).isEqualTo(ExporterType.OTLP);
        assertThat(properties.getOtlp()).isNotNull();
    }

    @Test
    void shouldUpdateType() {
        OpenTelemetryTracingExporterProperties properties = new OpenTelemetryTracingExporterProperties();

        properties.setType(ExporterType.NONE);

        assertThat(properties.getType()).isEqualTo(ExporterType.NONE);
    }

    @Test
    void shouldProvideAccessToOtlpConfig() {
        OpenTelemetryTracingExporterProperties properties = new OpenTelemetryTracingExporterProperties();

        assertThat(properties.getOtlp())
            .isNotNull()
            .isInstanceOf(io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpExporterConfig.class);
    }

}
