package io.arconia.opentelemetry.autoconfigure.sdk.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.sdk.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.OpenTelemetryMetricsProperties.ExemplarFilter;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.HistogramAggregationStrategy;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.OpenTelemetryTracingProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.OpenTelemetryTracingProperties.SamplingStrategy;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.OpenTelemetryTracingExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetrySdkPropertyAdapters}.
 */
class OpenTelemetrySdkPropertyAdaptersTests {

    @Test
    void generalShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyAdapters.general(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generalShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.sdk.disabled", "true")
            .withProperty("otel.attribute.value.length.limit", "100")
            .withProperty("otel.attribute.count.limit", "50")
            .withProperty("otel.propagators", "tracecontext,b3");

        var adapter = OpenTelemetrySdkPropertyAdapters.general(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isEqualTo(false);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length"))
            .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-attribute-value-length"))
            .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes"))
            .isEqualTo(50);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-attributes"))
            .isEqualTo(50);
        assertThat((List<PropagationType>) adapter.getArconiaProperties().get("management.tracing.propagation.produce"))
            .containsExactlyInAnyOrder(PropagationType.W3C, PropagationType.B3);
    }

    @Test
    void resourceShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyAdapters.resource(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void resourceShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.service.name", "test-service")
            .withProperty("otel.resource.attributes", "key1=value1,key2=value2")
            .withProperty("otel.resource.disabled.keys", "key1,key2");

        var adapter = OpenTelemetrySdkPropertyAdapters.resource(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")).isEqualTo("test-service");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".enable"))
            .isEqualTo(Map.of("key1", false, "key2", false));
    }

    @Test
    void logsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyAdapters.logs(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void logsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.blrp.schedule.delay", "5000")
            .withProperty("otel.blrp.max.queue.size", "2048")
            .withProperty("otel.blrp.max.export.batch.size", "512")
            .withProperty("otel.blrp.export.timeout", "30000");

        var adapter = OpenTelemetrySdkPropertyAdapters.logs(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.schedule-delay"))
            .isEqualTo(Duration.ofSeconds(5));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-queue-size"))
            .isEqualTo(2048);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size"))
            .isEqualTo(512);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.exporter-timeout"))
            .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void metricsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyAdapters.metrics(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void metricsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.metric.export.interval", "60000")
            .withProperty("otel.metrics.exemplar.filter", "always_on")
            .withProperty("otel.java.metrics.cardinality.limit", "100");

        var adapter = OpenTelemetrySdkPropertyAdapters.metrics(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".interval"))
            .isEqualTo(Duration.ofSeconds(60));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".exemplar-filter"))
            .isEqualTo(ExemplarFilter.ALWAYS_ON);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".cardinality-limit"))
                .isEqualTo(100);
    }

    @Test
    void tracesShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyAdapters.traces(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void tracesShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.bsp.schedule.delay", "5000")
            .withProperty("otel.bsp.max.queue.size", "2048")
            .withProperty("otel.bsp.max.export.batch.size", "512")
            .withProperty("otel.bsp.export.timeout", "30000")
            .withProperty("otel.tracer.sampler", "traceidratio")
            .withProperty("otel.tracer.sampler.arg", "0.5")
            .withProperty("otel.span.attribute.value.length.limit", "100")
            .withProperty("otel.span.attribute.count.limit", "50")
            .withProperty("otel.span.event.count.limit", "100")
            .withProperty("otel.span.link.count.limit", "100");

        var adapter = OpenTelemetrySdkPropertyAdapters.traces(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.schedule-delay"))
            .isEqualTo(Duration.ofSeconds(5));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-queue-size"))
            .isEqualTo(2048);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size"))
            .isEqualTo(512);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.exporter-timeout"))
            .isEqualTo(Duration.ofSeconds(30));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy"))
            .isEqualTo(SamplingStrategy.TRACE_ID_RATIO);
        assertThat(adapter.getArconiaProperties().get("management.tracing.sampling.probability"))
            .isEqualTo(0.5);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-attribute-value-length"))
            .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-attributes"))
            .isEqualTo(50);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-events"))
            .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-links"))
            .isEqualTo(100);
    }

    @Test
    void exportersShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyAdapters.exporters(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void exportersShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.logs.exporter", "otlp")
            .withProperty("otel.metrics.exporter", "otlp")
            .withProperty("otel.traces.exporter", "otlp")
            .withProperty("otel.java.exporter.memory_mode", "memory_mode")
            .withProperty("otel.exporter.otlp.protocol", "grpc")
            .withProperty("otel.exporter.otlp.endpoint", "http://localhost:4317")
            .withProperty("otel.exporter.otlp.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.compression", "gzip")
            .withProperty("otel.exporter.otlp.timeout", "10000")
            .withProperty("otel.exporter.otlp.metrics.default.histogram.aggregation", "EXPLICIT_BUCKET_HISTOGRAM")
            .withProperty("otel.exporter.otlp.metrics.temporality.preference", "DELTA");

        var adapter = OpenTelemetrySdkPropertyAdapters.exporters(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
            .isEqualTo(ExporterType.OTLP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".memoryMode"))
            .isEqualTo("memory_mode");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.protocol"))
            .isEqualTo(Protocol.GRPC);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4317");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.headers"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.compression"))
            .isEqualTo(Compression.GZIP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
            .isEqualTo(Duration.ofSeconds(10));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".histogram-aggregation"))
            .isEqualTo(HistogramAggregationStrategy.EXPLICIT_BUCKET_HISTOGRAM);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".aggregation-temporality"))
            .isEqualTo(AggregationTemporalityStrategy.DELTA);
    }

    @Test
    void exportersShouldMapLogsSpecificProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.exporter.otlp.logs.protocol", "http/protobuf")
            .withProperty("otel.exporter.otlp.logs.endpoint", "http://localhost:4318")
            .withProperty("otel.exporter.otlp.logs.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.logs.compression", "none")
            .withProperty("otel.exporter.otlp.logs.timeout", "20000");

        var adapter = OpenTelemetrySdkPropertyAdapters.exporters(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.protocol"))
            .isEqualTo(Protocol.HTTP_PROTOBUF);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4318");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.headers"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.compression"))
            .isEqualTo(Compression.NONE);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
            .isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void exportersShouldMapMetricsSpecificProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.exporter.otlp.metrics.protocol", "http/protobuf")
            .withProperty("otel.exporter.otlp.metrics.endpoint", "http://localhost:4318")
            .withProperty("otel.exporter.otlp.metrics.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.metrics.compression", "none")
            .withProperty("otel.exporter.otlp.metrics.timeout", "20000");

        var adapter = OpenTelemetrySdkPropertyAdapters.exporters(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.protocol"))
            .isEqualTo(Protocol.HTTP_PROTOBUF);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4318");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.headers"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.compression"))
            .isEqualTo(Compression.NONE);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
            .isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void exportersShouldMapTracesSpecificProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.exporter.otlp.traces.protocol", "http/protobuf")
            .withProperty("otel.exporter.otlp.traces.endpoint", "http://localhost:4318")
            .withProperty("otel.exporter.otlp.traces.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.traces.compression", "none")
            .withProperty("otel.exporter.otlp.traces.timeout", "20000");

        var adapter = OpenTelemetrySdkPropertyAdapters.exporters(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.protocol"))
            .isEqualTo(Protocol.HTTP_PROTOBUF);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint"))
            .isEqualTo("http://localhost:4318");
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.headers"))
            .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.compression"))
            .isEqualTo(Compression.NONE);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
            .isEqualTo(Duration.ofSeconds(20));
    }

}
