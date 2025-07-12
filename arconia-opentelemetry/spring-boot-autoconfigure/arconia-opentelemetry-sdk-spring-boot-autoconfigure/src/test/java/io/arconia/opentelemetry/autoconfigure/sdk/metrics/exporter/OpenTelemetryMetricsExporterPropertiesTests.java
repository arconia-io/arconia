package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter;

import org.junit.jupiter.api.Test;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;

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

        assertThat(properties.getType()).isNull();
        assertThat(properties.getAggregationTemporality()).isEqualTo(AggregationTemporalityStrategy.CUMULATIVE);
        assertThat(properties.getHistogramAggregation()).isEqualTo(HistogramAggregationStrategy.EXPLICIT_BUCKET_HISTOGRAM);
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
    void shouldUpdateHistogramAggregation() {
        OpenTelemetryMetricsExporterProperties properties = new OpenTelemetryMetricsExporterProperties();

        properties.setHistogramAggregation(HistogramAggregationStrategy.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);

        assertThat(properties.getHistogramAggregation()).isEqualTo(HistogramAggregationStrategy.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
    }

    @Test
    void shouldProvideAccessToOtlpConfig() {
        OpenTelemetryMetricsExporterProperties properties = new OpenTelemetryMetricsExporterProperties();

        assertThat(properties.getOtlp())
            .isNotNull()
            .isInstanceOf(io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.OtlpExporterConfig.class);
    }

}
