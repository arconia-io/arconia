package io.arconia.opentelemetry.autoconfigure.config;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.HistogramAggregationStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingProperties;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ActuatorPropertyAdapters}.
 */
class ActuatorPropertyAdaptersTests {

    @Test
    void resourceShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> ActuatorPropertyAdapters.resource(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void resourceShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("management.opentelemetry.resource-attributes", "key1=value1,key2=value2");

        var adapter = ActuatorPropertyAdapters.resource(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
    }

    @Test
    void logsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> ActuatorPropertyAdapters.logs(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void logsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("management.otlp.logging.export.enabled", "true")
            .withProperty("management.otlp.logging.transport", "grpc")
            .withProperty("management.otlp.logging.endpoint", "http://localhost:4317")
            .withProperty("management.otlp.logging.headers", "key1=value1,key2=value2")
            .withProperty("management.otlp.logging.compression", "gzip")
            .withProperty("management.otlp.logging.timeout", "10s");

        var adapter = ActuatorPropertyAdapters.logs(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.protocol"))
            .isEqualTo(Protocol.GRPC);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4317");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.headers"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.compression"))
            .isEqualTo(Compression.GZIP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
            .isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void metricsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> ActuatorPropertyAdapters.metrics(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void metricsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("management.otlp.metrics.export.enabled", "true")
            .withProperty("management.otlp.metrics.export.aggregation-temporality", "delta")
            .withProperty("management.otlp.metrics.export.headers", "key1=value1,key2=value2")
            .withProperty("management.otlp.metrics.export.histogram-flavor", "explicit_bucket_histogram")
            .withProperty("management.otlp.metrics.export.read-timeout", "10s")
            .withProperty("management.otlp.metrics.export.step", "60s")
            .withProperty("management.otlp.metrics.export.url", "http://localhost:4317");

        var adapter = ActuatorPropertyAdapters.metrics(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".aggregation-temporality"))
            .isEqualTo(AggregationTemporalityStrategy.DELTA);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.headers"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".histogram-aggregation"))
            .isEqualTo(HistogramAggregationStrategy.EXPLICIT_BUCKET_HISTOGRAM);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
            .isEqualTo(Duration.ofSeconds(10));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".interval"))
            .isEqualTo(Duration.ofSeconds(60));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4317");
    }

    @Test
    void tracesShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> ActuatorPropertyAdapters.traces(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void tracesShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("management.otlp.tracing.export.enabled", "true")
            .withProperty("management.otlp.tracing.transport", "grpc")
            .withProperty("management.otlp.tracing.endpoint", "http://localhost:4317")
            .withProperty("management.otlp.tracing.headers", "key1=value1,key2=value2")
            .withProperty("management.otlp.tracing.compression", "gzip")
            .withProperty("management.otlp.tracing.timeout", "10s");

        var adapter = ActuatorPropertyAdapters.traces(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".enabled"))
            .isEqualTo(true);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.protocol"))
            .isEqualTo(Protocol.GRPC);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4317");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.headers"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.compression"))
            .isEqualTo(Compression.GZIP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
            .isEqualTo(Duration.ofSeconds(10));
    }

}
