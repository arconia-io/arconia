package io.arconia.opentelemetry.autoconfigure.sdk.config;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.HistogramAggregationStrategy;

/**
 * Utility class for converting Actuator configuration types to Arconia configuration types.
 */
class ActuatorPropertyConverters {

    private static final Logger logger = LoggerFactory.getLogger(ActuatorPropertyConverters.class);

    static Function<String,@Nullable Protocol> protocol(String externalKey) {
        Assert.hasText(externalKey, "externalKey cannot be null or empty");
        return value -> {
            var protocol = switch (value.trim().toLowerCase()) {
                case "grpc" -> Protocol.GRPC;
                case "http" -> Protocol.HTTP_PROTOBUF;
                default -> null;
            };
            if (protocol == null) {
                logger.warn("Unsupported value for {}: {}", externalKey, value);
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
                logger.warn("Unsupported value for {}: {}", externalKey, value);
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
                logger.warn("Unsupported value for {}: {}", externalKey, value);
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
                default -> null;
            };
            if (aggregationTemporality == null) {
                logger.warn("Unsupported value for {}: {}", externalKey, value);
                return null;
            }
            return aggregationTemporality;
        };
    }

}
