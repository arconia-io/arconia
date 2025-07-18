package io.arconia.opentelemetry.autoconfigure.config;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.HistogramAggregationStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ActuatorPropertyConverters}.
 */
class ActuatorPropertyConvertersTests {

    @ParameterizedTest
    @CsvSource({
            "grpc, GRPC",
            "http, HTTP_PROTOBUF",
            "GRPC, GRPC",
            "HTTP, HTTP_PROTOBUF",
            "' grpc ', GRPC",
            "'\thttp\n', HTTP_PROTOBUF"
    })
    void protocolShouldConvertValidValues(String input, Protocol expected) {
        Function<String, Protocol> converter = ActuatorPropertyConverters.protocol("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void protocolShouldReturnNullForInvalidValue() {
        Function<String, Protocol> converter = ActuatorPropertyConverters.protocol("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void protocolShouldThrowExceptionForInvalidKey(String key) {
        assertThatThrownBy(() -> ActuatorPropertyConverters.protocol(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalKey cannot be null or empty");
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
        Function<String, Compression> converter = ActuatorPropertyConverters.compression("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void compressionShouldReturnNullForInvalidValue() {
        Function<String, Compression> converter = ActuatorPropertyConverters.compression("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void compressionShouldThrowExceptionForInvalidKey(String key) {
        assertThatThrownBy(() -> ActuatorPropertyConverters.compression(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalKey cannot be null or empty");
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
        Function<String, HistogramAggregationStrategy> converter = ActuatorPropertyConverters.histogramAggregation("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void histogramAggregationShouldReturnNullForInvalidValue() {
        Function<String, HistogramAggregationStrategy> converter = ActuatorPropertyConverters.histogramAggregation("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void histogramAggregationShouldThrowExceptionForInvalidKey(String key) {
        assertThatThrownBy(() -> ActuatorPropertyConverters.histogramAggregation(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalKey cannot be null or empty");
    }

    @ParameterizedTest
    @CsvSource({
            "CUMULATIVE, CUMULATIVE",
            "DELTA, DELTA",
            "cumulative, CUMULATIVE",
            "delta, DELTA",
            "' CUMULATIVE ', CUMULATIVE",
            "'\tDELTA\n', DELTA"
    })
    void aggregationTemporalityShouldConvertValidValues(String input, AggregationTemporalityStrategy expected) {
        Function<String, AggregationTemporalityStrategy> converter = ActuatorPropertyConverters.aggregationTemporality("test.key");
        assertThat(converter.apply(input)).isEqualTo(expected);
    }

    @Test
    void aggregationTemporalityShouldReturnNullForInvalidValue() {
        Function<String, AggregationTemporalityStrategy> converter = ActuatorPropertyConverters.aggregationTemporality("test.key");
        assertThat(converter.apply("invalid")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void aggregationTemporalityShouldThrowExceptionForInvalidKey(String key) {
        assertThatThrownBy(() -> ActuatorPropertyConverters.aggregationTemporality(key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("externalKey cannot be null or empty");
    }

}
