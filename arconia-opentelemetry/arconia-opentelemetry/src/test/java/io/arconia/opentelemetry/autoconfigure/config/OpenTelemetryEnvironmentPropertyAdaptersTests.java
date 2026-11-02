package io.arconia.opentelemetry.autoconfigure.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties.Propagation.PropagationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.HistogramAggregationStrategy;
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
@ExtendWith(OutputCaptureExtension.class)
class OpenTelemetryEnvironmentPropertyAdaptersTests {

    @Test
    @SuppressWarnings("unchecked")
    void generalShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.sdk.disabled", "true")
                .withProperty("otel.resource.attributes", "key1=value1,key2=value2")
                .withProperty("otel.service.name", "test-service")
                .withProperty("otel.propagators", "tracecontext,b3")
                .withProperty("otel.traces.sampler", "traceidratio")
                .withProperty("otel.traces.sampler.arg", "0.5");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isEqualTo(false);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes"))
                .isEqualTo(Map.of("key1", "value1", "key2", "value2"));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")).isEqualTo("test-service");
        assertThat((List<PropagationType>) adapter.getArconiaProperties().get("management.tracing.propagation.type"))
                .containsExactlyInAnyOrder(PropagationType.W3C, PropagationType.B3);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy"))
                .isEqualTo(SamplingStrategy.TRACE_ID_RATIO);
        assertThat(adapter.getArconiaProperties().get("management.tracing.sampling.probability"))
                .isEqualTo(0.5);
    }

    @Test
    void generalShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.general(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    @Test
    void shouldPercentDecodeResourceAttributeValues() {
        var environment = new MockEnvironment()
                .withProperty("otel.resource.attributes", "key1=a%2Cb%3Dc,key2=100%25,key3=a+b");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes"))
                .isEqualTo(Map.of("key1", "a,b=c", "key2", "100%", "key3", "a+b"));
    }

    @Test
    void batchSpanProcessorShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.bsp.schedule.delay", "5000")
                .withProperty("otel.bsp.max.queue.size", "2048")
                .withProperty("otel.bsp.max.export.batch.size", "512")
                .withProperty("otel.bsp.export.timeout", "30000");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.schedule-delay"))
                .isEqualTo(Duration.ofSeconds(5));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-queue-size"))
                .isEqualTo(2048);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size"))
                .isEqualTo(512);
        // Bound into the properties, so that a mapping to a property that doesn't exist fails.
        OpenTelemetryTracingProperties properties = new Binder(ConfigurationPropertySources.from(adapter.toPropertySource("test")))
                .bind(OpenTelemetryTracingProperties.CONFIG_PREFIX, OpenTelemetryTracingProperties.class)
                .get();
        assertThat(properties.getProcessor().getExportTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void batchSpanProcessorShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    @Test
    void logRecordProcessorShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.blrp.schedule.delay", "5000")
                .withProperty("otel.blrp.max.queue.size", "2048")
                .withProperty("otel.blrp.max.export.batch.size", "512")
                .withProperty("otel.blrp.export.timeout", "30000");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.schedule-delay"))
                .isEqualTo(Duration.ofSeconds(5));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-queue-size"))
                .isEqualTo(2048);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size"))
                .isEqualTo(512);
        // Bound into the properties, so that a mapping to a property that doesn't exist fails.
        OpenTelemetryLoggingProperties properties = new Binder(ConfigurationPropertySources.from(adapter.toPropertySource("test")))
                .bind(OpenTelemetryLoggingProperties.CONFIG_PREFIX, OpenTelemetryLoggingProperties.class)
                .get();
        assertThat(properties.getProcessor().getExportTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void logRecordProcessorShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    @Test
    @SuppressWarnings("unchecked")
    void attributeLimitsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.attribute.value.length.limit", "100")
            .withProperty("otel.attribute.count.limit", "50");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.attributeLimits(adapter(environment));

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
    void attributeLimitsShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.attributeLimits(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
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

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.spanLimits(adapter(environment));

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
    void spanLimitsShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.spanLimits(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    @Test
    void logRecordLimitsShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.logrecord.attribute.value.length.limit", "100")
                .withProperty("otel.logrecord.attribute.count.limit", "50");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.logRecordLimits(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length"))
                .isEqualTo(100);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes"))
                .isEqualTo(50);
    }

    @Test
    void logRecordLimitsShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.logRecordLimits(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    @Test
    void exporterSelectionShouldMapProperties() {
        var environment = new MockEnvironment()
                .withProperty("otel.logs.exporter", "otlp")
                .withProperty("otel.metrics.exporter", "otlp")
                .withProperty("otel.traces.exporter", "otlp");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type"))
                .isEqualTo(ExporterType.OTLP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type"))
                .isEqualTo(ExporterType.OTLP);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
                .isEqualTo(ExporterType.OTLP);
    }

    @Test
    void exporterSelectionShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    @Test
    void metricsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("otel.metrics.exemplar.filter", "always_on")
            .withProperty("otel.metric.export.interval", "60000")
            .withProperty("otel.metric.export.timeout", "60000");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.metrics(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".exemplars.filter"))
            .isEqualTo(ExemplarFilter.ALWAYS_ON);
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".interval"))
                .isEqualTo(Duration.ofSeconds(60));
        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout"))
                .isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void metricsShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.metrics(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
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

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment));

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
    void otlpExporterShouldThrowExceptionWhenAdapterIsNull() {
        assertThatThrownBy(() -> OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("adapter cannot be null");
    }

    // VALUES

    @ParameterizedTest
    @CsvSource({
            "console, CONSOLE",
            "none, NONE",
            "otlp, OTLP",
            "CONSOLE, CONSOLE",
            "' console ', CONSOLE",
            "'\tconsole\n', CONSOLE"
    })
    void shouldMapSupportedExporterTypes(String input, ExporterType expected) {
        var environment = new MockEnvironment().withProperty("otel.traces.exporter", input);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type"))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "grpc, GRPC",
            "http/protobuf, HTTP_PROTOBUF",
            "HTTP/PROTOBUF, HTTP_PROTOBUF",
            "' grpc ', GRPC"
    })
    void shouldMapSupportedProtocols(String input, Protocol expected) {
        var environment = new MockEnvironment().withProperty("otel.exporter.otlp.protocol", input);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.protocol"))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "gzip, GZIP",
            "none, NONE",
            "GZIP, GZIP",
            "' none ', NONE"
    })
    void shouldMapSupportedCompressions(String input, Compression expected) {
        var environment = new MockEnvironment().withProperty("otel.exporter.otlp.compression", input);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.compression"))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "always_on, ALWAYS_ON",
            "always_off, ALWAYS_OFF",
            "traceidratio, TRACE_ID_RATIO",
            "parentbased_always_on, PARENT_BASED_ALWAYS_ON",
            "parentbased_always_off, PARENT_BASED_ALWAYS_OFF",
            "parentbased_traceidratio, PARENT_BASED_TRACE_ID_RATIO",
            "ALWAYS_ON, ALWAYS_ON",
            "' always_on ', ALWAYS_ON"
    })
    void shouldMapSupportedSamplingStrategies(String input, SamplingStrategy expected) {
        var environment = new MockEnvironment().withProperty("otel.traces.sampler", input);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy"))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "always_on, ALWAYS_ON",
            "always_off, ALWAYS_OFF",
            "trace_based, TRACE_BASED",
            "TRACE_BASED, TRACE_BASED",
            "' always_on ', ALWAYS_ON"
    })
    void shouldMapSupportedExemplarFilters(String input, ExemplarFilter expected) {
        var environment = new MockEnvironment().withProperty("otel.metrics.exemplar.filter", input);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.metrics(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".exemplars.filter"))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "otel.traces.exporter",
            "otel.exporter.otlp.protocol",
            "otel.exporter.otlp.compression",
            "otel.traces.sampler",
            "otel.metrics.exemplar.filter",
            "otel.propagators",
            "otel.exporter.otlp.metrics.temporality.preference",
            "otel.exporter.otlp.metrics.default.histogram.aggregation"
    })
    void shouldSkipUnsupportedValues(String externalKey) {
        var environment = new MockEnvironment().withProperty(externalKey, "invalid");
        var adapter = adapter(environment);

        OpenTelemetryEnvironmentPropertyAdapters.general(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.metrics(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter);

        assertThat(adapter.getArconiaProperties()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapSupportedPropagatorsInOrderWithoutDuplicates() {
        var environment = new MockEnvironment().withProperty("otel.propagators", " B3 , tracecontext,baggage,invalid,b3multi");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat((List<PropagationType>) adapter.getArconiaProperties().get("management.tracing.propagation.type"))
                .containsExactly(PropagationType.B3, PropagationType.W3C, PropagationType.B3_MULTI);
    }

    // SPECIFICATION RULES

    @ParameterizedTest
    @CsvSource({ "true, false", "TRUE, false", "false, true" })
    void shouldMapSdkDisabledToEnabled(String disabled, boolean enabled) {
        var environment = new MockEnvironment().withProperty("otel.sdk.disabled", disabled);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")).isEqualTo(enabled);
    }

    @Test
    void shouldSkipInvalidSdkDisabled(CapturedOutput output) {
        var environment = new MockEnvironment().withProperty("otel.sdk.disabled", "yes");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties()).doesNotContainKey(OpenTelemetryProperties.CONFIG_PREFIX + ".enabled");
        assertThat(output).contains("Unsupported value for otel.sdk.disabled: yes");
    }

    @ParameterizedTest
    @ValueSource(strings = { "traceidratio", "parentbased_traceidratio" })
    void shouldMapSamplerArgumentForRatioBasedSamplers(String sampler) {
        var environment = new MockEnvironment()
                .withProperty("otel.traces.sampler", sampler)
                .withProperty("otel.traces.sampler.arg", "0.25");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties().get("management.tracing.sampling.probability")).isEqualTo(0.25);
    }

    @ParameterizedTest
    @ValueSource(strings = { "traceidratio", "parentbased_traceidratio" })
    void shouldDefaultRatioToOneForRatioBasedSamplersWithoutArgument(String sampler) {
        var environment = new MockEnvironment().withProperty("otel.traces.sampler", sampler);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties().get("management.tracing.sampling.probability")).isEqualTo(1.0);
    }

    @Test
    void shouldKeepConfiguredProbabilityForRatioBasedSamplerWithoutArgument() {
        var environment = new MockEnvironment()
                .withProperty("otel.traces.sampler", "traceidratio")
                .withProperty("management.tracing.sampling.probability", "0.5");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties()).doesNotContainKey("management.tracing.sampling.probability");
    }

    @ParameterizedTest
    @ValueSource(strings = { "-0.1", "1.1", "invalid" })
    void shouldSkipSamplerArgumentOutOfRange(String argument, CapturedOutput output) {
        var environment = new MockEnvironment()
                .withProperty("otel.traces.sampler", "traceidratio")
                .withProperty("otel.traces.sampler.arg", argument);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties()).doesNotContainKey("management.tracing.sampling.probability");
        assertThat(output).contains("Unsupported value for otel.traces.sampler.arg: " + argument);
    }

    @ParameterizedTest
    @ValueSource(strings = { "always_on", "invalid", "" })
    void shouldIgnoreSamplerArgumentForOtherSamplers(String sampler, CapturedOutput output) {
        var environment = new MockEnvironment()
                .withProperty("otel.traces.sampler", sampler)
                .withProperty("otel.traces.sampler.arg", "0.25");

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment));

        assertThat(adapter.getArconiaProperties()).doesNotContainKey("management.tracing.sampling.probability");
        assertThat(output).contains("Ignoring otel.traces.sampler.arg because it only applies to the traceidratio and parentbased_traceidratio samplers");
    }

    @Test
    void shouldMapNoPropagatorsToEmptyList(CapturedOutput output) {
        var environment = new MockEnvironment().withProperty("otel.propagators", "tracecontext,none");

        var propertySource = OpenTelemetryEnvironmentPropertyAdapters.general(adapter(environment)).toPropertySource("test");

        var produce = new Binder(ConfigurationPropertySources.from(propertySource))
                .bind("management.tracing.propagation.type", Bindable.listOf(PropagationType.class));
        assertThat(produce.get()).isEmpty();
        assertThat(output).contains("Ignoring the other values of otel.propagators because it includes 'none'");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "otel.bsp.export.timeout",
            "otel.blrp.export.timeout",
            "otel.metric.export.timeout",
            "otel.exporter.otlp.timeout",
            "otel.exporter.otlp.logs.timeout",
            "otel.exporter.otlp.metrics.timeout",
            "otel.exporter.otlp.traces.timeout"
    })
    void shouldMapZeroTimeoutToNoLimit(String externalKey) {
        var environment = new MockEnvironment().withProperty(externalKey, "0");
        var adapter = adapter(environment);

        OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.metrics(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter);

        assertThat(adapter.getArconiaProperties().values()).containsExactly(Duration.ofMillis(Integer.MAX_VALUE));
    }

    @ParameterizedTest
    @ValueSource(strings = { "otel.bsp.schedule.delay", "otel.blrp.schedule.delay", "otel.metric.export.interval" })
    void shouldKeepZeroDurations(String externalKey) {
        var environment = new MockEnvironment().withProperty(externalKey, "0");
        var adapter = adapter(environment);

        OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.metrics(adapter);

        assertThat(adapter.getArconiaProperties().values()).containsExactly(Duration.ZERO);
    }

    @ParameterizedTest
    @CsvSource({
            "cumulative, CUMULATIVE",
            "Delta, DELTA",
            "LowMemory, LOW_MEMORY"
    })
    void shouldMapSupportedAggregationTemporalities(String input, AggregationTemporalityStrategy expected) {
        var environment = new MockEnvironment().withProperty("otel.exporter.otlp.metrics.temporality.preference", input);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".aggregation-temporality"))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "explicit_bucket_histogram, EXPLICIT_BUCKET_HISTOGRAM",
            "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM, BASE2_EXPONENTIAL_BUCKET_HISTOGRAM"
    })
    void shouldMapSupportedHistogramAggregations(String input, HistogramAggregationStrategy expected) {
        var environment = new MockEnvironment().withProperty("otel.exporter.otlp.metrics.default.histogram.aggregation", input);

        var adapter = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment));

        assertThat(adapter.getArconiaProperties().get(OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".histogram-aggregation"))
                .isEqualTo(expected);
    }

    @Nested
    class CertificateVariables {

        private static final String BUNDLE = "spring.ssl.bundle.pem.otel-exporter-otlp";

        private static final String TRACES_BUNDLE = "spring.ssl.bundle.pem.otel-exporter-otlp-traces";

        @Test
        void certificateMappedToSslBundle() {
            var environment = new MockEnvironment()
                .withProperty("otel.exporter.otlp.certificate", "/etc/otel/ca.crt");

            Map<String, Object> properties = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment)).getArconiaProperties();

            assertThat(properties)
                .containsEntry(BUNDLE + ".truststore.certificate", "file:/etc/otel/ca.crt")
                .containsEntry(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.ssl.bundle", "otel-exporter-otlp")
                .doesNotContainKeys(BUNDLE + ".keystore.certificate", BUNDLE + ".keystore.private-key")
                .doesNotContainKey(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.ssl.bundle");
        }

        @Test
        void clientKeyAndCertificateMappedToSslBundle() {
            var environment = new MockEnvironment()
                .withProperty("otel.exporter.otlp.client.key", "/etc/otel/client.key")
                .withProperty("otel.exporter.otlp.client.certificate", "/etc/otel/client.crt");

            Map<String, Object> properties = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment)).getArconiaProperties();

            assertThat(properties)
                .containsEntry(BUNDLE + ".keystore.private-key", "file:/etc/otel/client.key")
                .containsEntry(BUNDLE + ".keystore.certificate", "file:/etc/otel/client.crt")
                .containsEntry(OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.ssl.bundle", "otel-exporter-otlp")
                .doesNotContainKey(BUNDLE + ".truststore.certificate");
        }

        @Test
        void clientKeyWithoutCertificateIgnored(CapturedOutput output) {
            var environment = new MockEnvironment()
                .withProperty("otel.exporter.otlp.client.key", "/etc/otel/client.key");

            Map<String, Object> properties = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment)).getArconiaProperties();

            assertThat(properties).doesNotContainKeys(BUNDLE + ".keystore.private-key",
                    OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.ssl.bundle");
            assertThat(output).contains("Ignoring otel.exporter.otlp.client.key because a client key and a client certificate are both required");
        }

        @Test
        void signalVariablesFallBackToCommonVariables() {
            var environment = new MockEnvironment()
                .withProperty("otel.exporter.otlp.certificate", "/etc/otel/ca.crt")
                .withProperty("otel.exporter.otlp.client.key", "/etc/otel/client.key")
                .withProperty("otel.exporter.otlp.client.certificate", "/etc/otel/client.crt")
                .withProperty("otel.exporter.otlp.traces.certificate", "/etc/otel/traces-ca.crt");

            Map<String, Object> properties = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment)).getArconiaProperties();

            assertThat(properties)
                .containsEntry(TRACES_BUNDLE + ".truststore.certificate", "file:/etc/otel/traces-ca.crt")
                .containsEntry(TRACES_BUNDLE + ".keystore.private-key", "file:/etc/otel/client.key")
                .containsEntry(TRACES_BUNDLE + ".keystore.certificate", "file:/etc/otel/client.crt")
                .containsEntry(OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.ssl.bundle", "otel-exporter-otlp-traces")
                .containsEntry(BUNDLE + ".truststore.certificate", "file:/etc/otel/ca.crt")
                .doesNotContainKey(OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.ssl.bundle")
                .doesNotContainKey("spring.ssl.bundle.pem.otel-exporter-otlp-logs.truststore.certificate");
        }

        @Test
        void resourceLocationsKeptAsTheyAre() {
            var environment = new MockEnvironment()
                .withProperty("otel.exporter.otlp.certificate", "classpath:ssl/ca.crt");

            Map<String, Object> properties = OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter(environment)).getArconiaProperties();

            assertThat(properties).containsEntry(BUNDLE + ".truststore.certificate", "classpath:ssl/ca.crt");
        }

    }

    private static OpenTelemetryEnvironmentPropertyAdapter adapter(MockEnvironment environment) {
        return new OpenTelemetryEnvironmentPropertyAdapter(environment, LogFactory.getLog(OpenTelemetryEnvironmentPropertyAdaptersTests.class));
    }

}
