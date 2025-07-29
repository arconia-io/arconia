package io.arconia.opentelemetry.autoconfigure.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties.ExemplarFilter;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingProperties;
import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingProperties.SamplingStrategy;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.OpenTelemetryTracingExporterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetryEnvironmentPropertyAdapters}.
 */
class OpenTelemetryEnvironmentPropertyAdaptersTests {

    @Test
    @SuppressWarnings("unchecked")
    void generalShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.sdk.disabled", "true")
                .withProperty("otel.resource.attributes", "key1=value1,key2=value2")
                .withProperty("otel.service.name", "test-service")
                .withProperty("otel.propagators", "tracecontext,b3")
                .withProperty("otel.tracer.sampler", "traceidratio")
                .withProperty("otel.tracer.sampler.arg", "0.5");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isEqualTo(false);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes"))
                .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")).isEqualTo("test-service");
        assertThat((List<PropagationType>) adapter.getArconiaProperties().get("management.tracing.propagation.produce"))
                .containsExactlyInAnyOrder(PropagationType.W3C, PropagationType.B3);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy"))
                .isEqualTo(SamplingStrategy.TRACE_ID_RATIO);
        assertThat(adapter.getArconiaProperties().get("management.tracing.sampling.probability"))
                .isEqualTo(0.5);
    }

    @Test
    void generalShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.general(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void batchSpanProcessorShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.bsp.schedule.delay", "5000")
                .withProperty("otel.bsp.max.queue.size", "2048")
                .withProperty("otel.bsp.max.export.batch.size", "512")
                .withProperty("otel.bsp.export.timeout", "30000");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.schedule-delay"))
                .isEqualTo(Duration.ofSeconds(5));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-queue-size"))
                .isEqualTo(2048);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size"))
                .isEqualTo(512);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.exporter-timeout"))
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void batchSpanProcessorShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void logRecordProcessorShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.blrp.schedule.delay", "5000")
                .withProperty("otel.blrp.max.queue.size", "2048")
                .withProperty("otel.blrp.max.export.batch.size", "512")
                .withProperty("otel.blrp.export.timeout", "30000");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(environment);

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
    void logRecordProcessorShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    @SuppressWarnings("unchecked")
    void attributeLimitsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.attribute.value.length.limit", "100")
            .withProperty("otel.attribute.count.limit", "50");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.attributeLimits(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length"))
            .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length"))
            .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes"))
            .isEqualTo(50);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes"))
            .isEqualTo(50);
    }

    @Test
    void attributeLimitsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.attributeLimits(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void spanLimitsShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.span.attribute.value.length.limit", "100")
                .withProperty("otel.span.attribute.count.limit", "50")
                .withProperty("otel.span.event.count.limit", "100")
                .withProperty("otel.span.link.count.limit", "100")
                .withProperty("otel.event.attribute.count.limit", "100")
                .withProperty("otel.link.attribute.count.limit", "100");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.spanLimits(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length"))
                .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes"))
                .isEqualTo(50);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-events"))
                .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-links"))
                .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes-per-event"))
                .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes-per-link"))
                .isEqualTo(100);
    }

    @Test
    void spanLimitsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.spanLimits(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void logRecordLimitsShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.logrecord.attribute.value.length.limit", "100")
                .withProperty("otel.logrecord.attribute.count.limit", "50");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.logRecordLimits(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length"))
                .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes"))
                .isEqualTo(50);
    }

    @Test
    void logRecordLimitsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.logRecordLimits(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void exporterSelectionShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.logs.exporter", "otlp")
                .withProperty("otel.metrics.exporter", "otlp")
                .withProperty("otel.traces.exporter", "otlp");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
                .isEqualTo(ExporterType.OTLP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
                .isEqualTo(ExporterType.OTLP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
                .isEqualTo(ExporterType.OTLP);
    }

    @Test
    void exporterSelectionShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void metricsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.metrics.exemplar.filter", "always_on")
            .withProperty("otel.metric.export.interval", "60000")
            .withProperty("otel.metric.export.timeout", "60000");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.metrics(environment);

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".exemplars.filter"))
            .isEqualTo(ExemplarFilter.ALWAYS_ON);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".interval"))
                .isEqualTo(Duration.ofSeconds(60));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
                .isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void metricsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.metrics(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void otlpExporterShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.exporter.otlp.protocol", "grpc")
            .withProperty("otel.exporter.otlp.endpoint", "http://localhost:4317")
            .withProperty("otel.exporter.otlp.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.compression", "gzip")
            .withProperty("otel.exporter.otlp.timeout", "10000")

            .withProperty("otel.exporter.otlp.logs.protocol", "http/protobuf")
            .withProperty("otel.exporter.otlp.logs.endpoint", "http://localhost:4318")
            .withProperty("otel.exporter.otlp.logs.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.logs.compression", "none")
            .withProperty("otel.exporter.otlp.logs.timeout", "20000")

            .withProperty("otel.exporter.otlp.metrics.protocol", "http/protobuf")
            .withProperty("otel.exporter.otlp.metrics.endpoint", "http://localhost:4318")
            .withProperty("otel.exporter.otlp.metrics.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.metrics.compression", "none")
            .withProperty("otel.exporter.otlp.metrics.timeout", "20000")

            .withProperty("otel.exporter.otlp.traces.protocol", "http/protobuf")
            .withProperty("otel.exporter.otlp.traces.endpoint", "http://localhost:4318")
            .withProperty("otel.exporter.otlp.traces.headers", "key1=value1,key2=value2")
            .withProperty("otel.exporter.otlp.traces.compression", "none")
            .withProperty("otel.exporter.otlp.traces.timeout", "20000");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(environment);

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

    @Test
    void otlpExporterShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }


}
