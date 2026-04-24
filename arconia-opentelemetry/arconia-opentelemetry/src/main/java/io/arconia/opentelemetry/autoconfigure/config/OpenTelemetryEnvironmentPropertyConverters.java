package io.arconia.opentelemetry.autoconfigure.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties.Propagation.PropagationType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties.ExemplarFilter;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.HistogramAggregationStrategy;
import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingProperties.SamplingStrategy;

/**
 * Utility class for converting OpenTelemetry Environment Variable Specification configuration types to Arconia configuration types.
 */
class OpenTelemetryEnvironmentPropertyConverters {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryEnvironmentPropertyConverters.class);

    static Function<String,@Nullable ExporterType> exporterType(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var exporterType = switch (value.trim().toLowerCase()) {
                case "console" -> ExporterType.CONSOLE;
                case "none" -> ExporterType.NONE;
                case "otlp" -> ExporterType.OTLP;
                default -> null;
            };
            if (exporterType == null) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
            return exporterType;
        };
    }

    static Function<String,@Nullable Protocol> protocol(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var protocol = switch (value.trim().toLowerCase()) {
                case "grpc" -> Protocol.GRPC;
                case "http/protobuf" -> Protocol.HTTP_PROTOBUF;
                default -> null;
            };
            if (protocol == null) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
            return protocol;
        };
    }

    static Function<String,@Nullable Compression> compression(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var compression = switch (value.trim().toLowerCase()) {
                case "gzip" -> Compression.GZIP;
                case "none" -> Compression.NONE;
                default -> null;
            };
            if (compression == null) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
            return compression;
        };
    }

    static Function<String,@Nullable HistogramAggregationStrategy> histogramAggregation(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var histogramAggregation = switch (value.trim().toUpperCase()) {
                case "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM" -> HistogramAggregationStrategy.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM;
                case "EXPLICIT_BUCKET_HISTOGRAM" -> HistogramAggregationStrategy.EXPLICIT_BUCKET_HISTOGRAM;
                default -> null;
            };
            if (histogramAggregation == null) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
            return histogramAggregation;
        };
    }

    static Function<String,@Nullable AggregationTemporalityStrategy> aggregationTemporality(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var aggregationTemporality = switch (value.trim().toUpperCase()) {
                case "CUMULATIVE" -> AggregationTemporalityStrategy.CUMULATIVE;
                case "DELTA" -> AggregationTemporalityStrategy.DELTA;
                case "LOWMEMORY" -> AggregationTemporalityStrategy.LOW_MEMORY;
                default -> null;
            };
            if (aggregationTemporality == null) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
            return aggregationTemporality;
        };
    }

    static Function<String,@Nullable SamplingStrategy> samplingStrategy(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var protocol = switch (value.toLowerCase().trim()) {
                case "always_on" -> SamplingStrategy.ALWAYS_ON;
                case "always_off" -> SamplingStrategy.ALWAYS_OFF;
                case "traceidratio" -> SamplingStrategy.TRACE_ID_RATIO;
                case "parentbased_always_on" -> SamplingStrategy.PARENT_BASED_ALWAYS_ON;
                case "parentbased_always_off" -> SamplingStrategy.PARENT_BASED_ALWAYS_OFF;
                case "parentbased_traceidratio" -> SamplingStrategy.PARENT_BASED_TRACE_ID_RATIO;
                default -> null;
            };
            if (protocol == null) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
            return protocol;
        };
    }

    static Function<String,@Nullable List<PropagationType>> propagationType(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            Set<PropagationType> propagators = new HashSet<>();
            String[] items = value.trim().toLowerCase().split("\\s*,\\s*");
            for (String item : items) {
                var propagator = switch (item.trim()) {
                    case "baggage" -> PropagationType.W3C;
                    case "tracecontext" -> PropagationType.W3C;
                    case "b3" -> PropagationType.B3;
                    case "b3multi" -> PropagationType.B3_MULTI;
                    default -> null;
                };
                if (propagator == null) {
                    logUnsupportedValue(externalKey, value);
                } else {
                    propagators.add(propagator);
                }
            }
            List<PropagationType> propagatorLists = propagators.stream().toList();
            return CollectionUtils.isEmpty(propagatorLists) ? null : propagatorLists;
        };
    }

    static Function<String,@Nullable ExemplarFilter> exemplarFilter(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var exemplarFilter = switch (value.trim().toLowerCase()) {
                case "always_on" -> ExemplarFilter.ALWAYS_ON;
                case "always_off" -> ExemplarFilter.ALWAYS_OFF;
                case "trace_based" -> ExemplarFilter.TRACE_BASED;
                default -> null;
            };
            if (exemplarFilter == null) {
                logUnsupportedValue(externalKey, value);
                return null;
            }
            return exemplarFilter;
        };
    }

    private static void logUnsupportedValue(String externalKey, String value) {
        logger.warn("Unsupported value for {}: {}", externalKey, value);
    }

}
