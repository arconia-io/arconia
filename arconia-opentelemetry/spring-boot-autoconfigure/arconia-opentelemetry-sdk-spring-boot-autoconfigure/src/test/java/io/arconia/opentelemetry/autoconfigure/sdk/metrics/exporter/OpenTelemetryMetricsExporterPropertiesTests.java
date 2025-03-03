package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryMetricsExporterProperties}.
 */
class OpenTelemetryMetricsExporterPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.metrics.exporter");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenTelemetryMetricsExporterProperties properties = new OpenTelemetryMetricsExporterProperties();

        assertThat(properties.getType()).isEqualTo(ExporterType.OTLP);
        assertThat(properties.getAggregationTemporality()).isEqualTo(AggregationTemporalityStrategy.CUMULATIVE);
        assertThat(properties.getOtlp()).isNotNull();
    }

    @Test
    void shouldUpdateType() {
        OpenTelemetryMetricsExporterProperties properties = new OpenTelemetryMetricsExporterProperties();

        properties.setType(ExporterType.NONE);

        assertThat(properties.getType()).isEqualTo(ExporterType.NONE);
    }

    @Test
    void shouldUpdateAggregationTemporality() {
        OpenTelemetryMetricsExporterProperties properties = new OpenTelemetryMetricsExporterProperties();

        properties.setAggregationTemporality(AggregationTemporalityStrategy.DELTA);

        assertThat(properties.getAggregationTemporality()).isEqualTo(AggregationTemporalityStrategy.DELTA);
    }

    @Test
    void shouldProvideAccessToOtlpConfig() {
        OpenTelemetryMetricsExporterProperties properties = new OpenTelemetryMetricsExporterProperties();

        assertThat(properties.getOtlp())
            .isNotNull()
            .isInstanceOf(io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpExporterConfig.class);
    }

}
