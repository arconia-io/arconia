package io.arconia.opentelemetry.autoconfigure.config;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType;

import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties.ExemplarFilter;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.HistogramAggregationStrategy;
import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingProperties.SamplingStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetrySdkPropertyConverters}.
 */
class OpenTelemetrySdkPropertyConvertersTests {

    @ParameterizedTest
    @CsvSource({
            "console, CONSOLE",
            "none, NONE",
            "otlp, OTLP",
            "CONSOLE, CONSOLE",
            "NONE, NONE",
            "OTLP, OTLP",
            "' console ', CONSOLE",
            "'\tconsole\n', CONSOLE"
    })
    void exporterTypeShouldConvertValidValues(String input, ExporterType expected) {
        Function<String, ExporterType> converter = OpenTelemetrySdkPropertyConverters.exporterType("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void exporterTypeShouldReturnNullForInvalidValue() {
        Function<String, ExporterType> converter = OpenTelemetrySdkPropertyConverters.exporterType("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void exporterTypeShouldThrowExceptionForInvalidKey(String key) {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyConverters.exporterType(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalKey cannot be null or empty");
    }

    @ParameterizedTest
    @CsvSource({
            "grpc, GRPC",
            "http/protobuf, HTTP_PROTOBUF",
            "GRPC, GRPC",
            "HTTP/PROTOBUF, HTTP_PROTOBUF",
            "' grpc ', GRPC",
            "'\thttp/protobuf\n', HTTP_PROTOBUF"
    })
    void protocolShouldConvertValidValues(String input, Protocol expected) {
        Function<String, Protocol> converter = OpenTelemetrySdkPropertyConverters.protocol("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void protocolShouldReturnNullForInvalidValue() {
        Function<String, Protocol> converter = OpenTelemetrySdkPropertyConverters.protocol("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "gzip, GZIP",
            "none, NONE",
            "GZIP, GZIP",
            "NONE, NONE",
            "' gzip ', GZIP",
            "'\tnone\n', NONE"
    })
    void compressionShouldConvertValidValues(String input, Compression expected) {
        Function<String, Compression> converter = OpenTelemetrySdkPropertyConverters.compression("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void compressionShouldReturnNullForInvalidValue() {
        Function<String, Compression> converter = OpenTelemetrySdkPropertyConverters.compression("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM, BASE2_EXPONENTIAL_BUCKET_HISTOGRAM",
            "EXPLICIT_BUCKET_HISTOGRAM, EXPLICIT_BUCKET_HISTOGRAM",
            "base2_exponential_bucket_histogram, BASE2_EXPONENTIAL_BUCKET_HISTOGRAM",
            "explicit_bucket_histogram, EXPLICIT_BUCKET_HISTOGRAM",
            "' BASE2_EXPONENTIAL_BUCKET_HISTOGRAM ', BASE2_EXPONENTIAL_BUCKET_HISTOGRAM",
            "'\tEXPLICIT_BUCKET_HISTOGRAM\n', EXPLICIT_BUCKET_HISTOGRAM"
    })
    void histogramAggregationShouldConvertValidValues(String input, HistogramAggregationStrategy expected) {
        Function<String, HistogramAggregationStrategy> converter = OpenTelemetrySdkPropertyConverters.histogramAggregation("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void histogramAggregationShouldReturnNullForInvalidValue() {
        Function<String, HistogramAggregationStrategy> converter = OpenTelemetrySdkPropertyConverters.histogramAggregation("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "CUMULATIVE, CUMULATIVE",
            "DELTA, DELTA",
            "LOWMEMORY, LOW_MEMORY",
            "cumulative, CUMULATIVE",
            "delta, DELTA",
            "lowmemory, LOW_MEMORY",
            "' CUMULATIVE ', CUMULATIVE",
            "'\tDELTA\n', DELTA"
    })
    void aggregationTemporalityShouldConvertValidValues(String input, AggregationTemporalityStrategy expected) {
        Function<String, AggregationTemporalityStrategy> converter = OpenTelemetrySdkPropertyConverters.aggregationTemporality("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void aggregationTemporalityShouldReturnNullForInvalidValue() {
        Function<String, AggregationTemporalityStrategy> converter = OpenTelemetrySdkPropertyConverters.aggregationTemporality("test.key");
        assertThat(converter.apply("invalid")).isNull();
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
            "ALWAYS_OFF, ALWAYS_OFF",
            "' always_on ', ALWAYS_ON",
            "'\ttraceidratio\n', TRACE_ID_RATIO"
    })
    void samplingStrategyShouldConvertValidValues(String input, SamplingStrategy expected) {
        Function<String, SamplingStrategy> converter = OpenTelemetrySdkPropertyConverters.samplingStrategy("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void samplingStrategyShouldReturnNullForInvalidValue() {
        Function<String, SamplingStrategy> converter = OpenTelemetrySdkPropertyConverters.samplingStrategy("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "baggage",
            "tracecontext",
            "b3",
            "b3multi",
            "baggage,tracecontext",
            "b3,b3multi",
            "baggage,tracecontext,b3,b3multi",
            " baggage , tracecontext ",
            " b3 , b3multi "
    })
    void propagationTypeShouldConvertValidValues(String input) {
        Function<String, List<PropagationType>> converter = OpenTelemetrySdkPropertyConverters.propagationType("test.key");
        List<PropagationType> result = converter.apply(input);
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(type -> type == PropagationType.W3C || type == PropagationType.B3 || type == PropagationType.B3_MULTI);
    }

    @Test
    void propagationTypeShouldHandleInvalidValue() {
        Function<String, List<PropagationType>> converter = OpenTelemetrySdkPropertyConverters.propagationType("test.key");
        List<PropagationType> result = converter.apply("invalid");
        assertThat(result).isNull();
    }

    @Test
    void propagationTypeShouldFilterInvalidValues() {
        Function<String, List<PropagationType>> converter = OpenTelemetrySdkPropertyConverters.propagationType("test.key");
        List<PropagationType> result = converter.apply("baggage,invalid,b3");
        assertThat(result).containsExactlyInAnyOrder(PropagationType.W3C, PropagationType.B3);
    }

    @ParameterizedTest
    @CsvSource({
            "always_on, ALWAYS_ON",
            "always_off, ALWAYS_OFF",
            "trace_based, TRACE_BASED",
            "ALWAYS_ON, ALWAYS_ON",
            "ALWAYS_OFF, ALWAYS_OFF",
            "TRACE_BASED, TRACE_BASED",
            "' always_on ', ALWAYS_ON",
            "'\ttrace_based\n', TRACE_BASED"
    })
    void exemplarFilterShouldConvertValidValues(String input, ExemplarFilter expected) {
        Function<String, ExemplarFilter> converter = OpenTelemetrySdkPropertyConverters.exemplarFilter("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void exemplarFilterShouldReturnNullForInvalidValue() {
        Function<String, ExemplarFilter> converter = OpenTelemetrySdkPropertyConverters.exemplarFilter("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void exemplarFilterShouldThrowExceptionForInvalidKey(String key) {
        assertThatThrownBy(() -> OpenTelemetrySdkPropertyConverters.exemplarFilter(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalKey cannot be null or empty");
    }

}
