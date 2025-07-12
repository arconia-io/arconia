package io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter;

import org.junit.jupiter.api.Test;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryLoggingExporterProperties}.
 */
class OpenTelemetryLoggingExporterPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.logs.exporter");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenTelemetryLoggingExporterProperties properties = new OpenTelemetryLoggingExporterProperties();

        assertThat(properties.getType()).isNull();
        assertThat(properties.getOtlp()).isNotNull();
    }

    @Test
    void shouldUpdateType() {
        OpenTelemetryLoggingExporterProperties properties = new OpenTelemetryLoggingExporterProperties();

        properties.setType(ExporterType.NONE);

        assertThat(properties.getType()).isEqualTo(ExporterType.NONE);
    }

    @Test
    void shouldProvideAccessToOtlpConfig() {
        OpenTelemetryLoggingExporterProperties properties = new OpenTelemetryLoggingExporterProperties();

        assertThat(properties.getOtlp())
            .isNotNull()
            .isInstanceOf(io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpExporterConfig.class);
    }

}
