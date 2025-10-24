package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import org.junit.jupiter.api.Test;

import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpExporterConfig;

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
        assertThat(properties.getOtlp()).isNotNull().isInstanceOf(OtlpExporterConfig.class);
    }

    @Test
    void shouldUpdateValuese() {
        OpenTelemetryMetricsExporterProperties properties = new OpenTelemetryMetricsExporterProperties();

        properties.setType(ExporterType.NONE);
        properties.setAggregationTemporality(AggregationTemporalityStrategy.DELTA);
        properties.setHistogramAggregation(HistogramAggregationStrategy.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);

        assertThat(properties.getType()).isEqualTo(ExporterType.NONE);
        assertThat(properties.getAggregationTemporality()).isEqualTo(AggregationTemporalityStrategy.DELTA);
        assertThat(properties.getHistogramAggregation()).isEqualTo(HistogramAggregationStrategy.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
    }

}
