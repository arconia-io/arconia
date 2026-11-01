package io.arconia.opentelemetry.autoconfigure.config;

import java.util.Map;

import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties.Propagation.PropagationType;
import org.springframework.util.Assert;

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

/**
 * Provides adapters from OpenTelemetry Environment Variable Specification to Arconia properties.
 * It relies on Spring Boot's relaxed binding capabilities to map environment variables,
 * JVM system properties, command-line arguments, and properties to the appropriate Arconia configuration.
 * That's especially useful if you're migrating from the OpenTelemetry Spring Boot Starter to Arconia OpenTelemetry.
 *
 * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/">OpenTelemetry Environment Variable Specification</a>
 */
class OpenTelemetryEnvironmentPropertyAdapters {

    private static final Map<String, PropagationType> PROPAGATION_TYPES = Map.of(
            "tracecontext", PropagationType.W3C,
            "baggage", PropagationType.W3C,
            "b3", PropagationType.B3,
            "b3multi", PropagationType.B3_MULTI);

    private static final Map<String, SamplingStrategy> SAMPLING_STRATEGIES = Map.of(
            "always_on", SamplingStrategy.ALWAYS_ON,
            "always_off", SamplingStrategy.ALWAYS_OFF,
            "traceidratio", SamplingStrategy.TRACE_ID_RATIO,
            "parentbased_always_on", SamplingStrategy.PARENT_BASED_ALWAYS_ON,
            "parentbased_always_off", SamplingStrategy.PARENT_BASED_ALWAYS_OFF,
            "parentbased_traceidratio", SamplingStrategy.PARENT_BASED_TRACE_ID_RATIO);

    private static final Map<String, ExporterType> EXPORTER_TYPES = Map.of(
            "console", ExporterType.CONSOLE,
            "none", ExporterType.NONE,
            "otlp", ExporterType.OTLP);

    private static final Map<String, ExemplarFilter> EXEMPLAR_FILTERS = Map.of(
            "always_on", ExemplarFilter.ALWAYS_ON,
            "always_off", ExemplarFilter.ALWAYS_OFF,
            "trace_based", ExemplarFilter.TRACE_BASED);

    private static final Map<String, Protocol> PROTOCOLS = Map.of(
            "grpc", Protocol.GRPC,
            "http/protobuf", Protocol.HTTP_PROTOBUF);

    private static final Map<String, Compression> COMPRESSIONS = Map.of(
            "gzip", Compression.GZIP,
            "none", Compression.NONE);

    private static final Map<String, AggregationTemporalityStrategy> AGGREGATION_TEMPORALITIES = Map.of(
            "cumulative", AggregationTemporalityStrategy.CUMULATIVE,
            "delta", AggregationTemporalityStrategy.DELTA,
            "lowmemory", AggregationTemporalityStrategy.LOW_MEMORY);

    private static final Map<String, HistogramAggregationStrategy> HISTOGRAM_AGGREGATIONS = Map.of(
            "explicit_bucket_histogram", HistogramAggregationStrategy.EXPLICIT_BUCKET_HISTOGRAM,
            "base2_exponential_bucket_histogram", HistogramAggregationStrategy.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);

    private static final String SAMPLING_STRATEGY_KEY = OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy";

    /**
     * General SDK Configuration.
     * <p>
     * All environment variables are supported except:
     * <ul>
     *     <li>{@code OTEL_LOG_LEVEL}</li>
     * </ul>
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#general-sdk-configuration">General SDK Configuration</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter general(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        adapter
                // OTEL_SDK_DISABLED
                .mapInverseBoolean("otel.sdk.disabled", OpenTelemetryProperties.CONFIG_PREFIX + ".enabled")
                // OTEL_RESOURCE_ATTRIBUTES
                .mapMap("otel.resource.attributes", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes")
                // OTEL_SERVICE_NAME
                .mapString("otel.service.name", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")
                // OTEL_PROPAGATORS
                .mapEnumList("otel.propagators", "management.tracing.propagation.produce", PROPAGATION_TYPES)
                // OTEL_TRACES_SAMPLER
                .mapEnum("otel.traces.sampler", SAMPLING_STRATEGY_KEY, SAMPLING_STRATEGIES);

        // OTEL_TRACES_SAMPLER_ARG, only defined for the ratio-based samplers
        Object samplingStrategy = adapter.getArconiaProperties().get(SAMPLING_STRATEGY_KEY);
        if (samplingStrategy == SamplingStrategy.TRACE_ID_RATIO || samplingStrategy == SamplingStrategy.PARENT_BASED_TRACE_ID_RATIO) {
            return adapter.mapDouble("otel.traces.sampler.arg", "management.tracing.sampling.probability", 0.0, 1.0);
        }
        return adapter.ignoreProperty("otel.traces.sampler.arg",
                "it only applies to the traceidratio and parentbased_traceidratio samplers set via otel.traces.sampler");
    }

    /**
     * Batch Span Processor.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#batch-span-processor">Batch Span Processor</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter batchSpanProcessor(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter
                // OTEL_BSP_SCHEDULE_DELAY
                .mapDuration("otel.bsp.schedule.delay", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.schedule-delay")
                // OTEL_BSP_EXPORT_TIMEOUT
                .mapTimeout("otel.bsp.export.timeout", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.exporter-timeout")
                // OTEL_BSP_MAX_QUEUE_SIZE
                .mapInteger("otel.bsp.max.queue.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-queue-size")
                // OTEL_BSP_MAX_EXPORT_BATCH_SIZE
                .mapInteger("otel.bsp.max.export.batch.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size");
    }

    /**
     * Batch Log Record Processor.
     * <p>
     * All properties supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#batch-logrecord-processor">Batch Log Record Processor</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter logRecordProcessor(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter
                // OTEL_BLRP_SCHEDULE_DELAY
                .mapDuration("otel.blrp.schedule.delay", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.schedule-delay")
                // OTEL_BLRP_EXPORT_TIMEOUT
                .mapTimeout("otel.blrp.export.timeout", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.exporter-timeout")
                // OTEL_BLRP_MAX_QUEUE_SIZE
                .mapInteger("otel.blrp.max.queue.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-queue-size")
                // OTEL_BLRP_MAX_EXPORT_BATCH_SIZE
                .mapInteger("otel.blrp.max.export.batch.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size");
    }

    /**
     * Attribute Limits.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#attribute-limits">Attribute Limits</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter attributeLimits(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter
                // OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT
                .mapInteger("otel.attribute.value.length.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length")
                .mapInteger("otel.attribute.value.length.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length")
                // OTEL_ATTRIBUTE_COUNT_LIMIT
                .mapInteger("otel.attribute.count.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes")
                .mapInteger("otel.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes");
    }

    /**
     * Span Limits.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#span-limits">Span Limits</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter spanLimits(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter
                // OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT
                .mapInteger("otel.span.attribute.value.length.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length")
                // OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT
                .mapInteger("otel.span.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes")
                // OTEL_SPAN_EVENT_COUNT_LIMIT
                .mapInteger("otel.span.event.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-events")
                // OTEL_SPAN_LINK_COUNT_LIMIT
                .mapInteger("otel.span.link.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-links")
                // OTEL_EVENT_ATTRIBUTE_COUNT_LIMIT
                .mapInteger("otel.event.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes-per-event")
                // OTEL_LINK_ATTRIBUTE_COUNT_LIMIT
                .mapInteger("otel.link.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes-per-link");
    }

    /**
     * Log Record Limits.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#logrecord-limits">Log Record Limits</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter logRecordLimits(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter
                // OTEL_LOGRECORD_ATTRIBUTE_VALUE_LENGTH_LIMIT
                .mapInteger("otel.logrecord.attribute.value.length.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length")
                // OTEL_LOGRECORD_ATTRIBUTE_COUNT_LIMIT
                .mapInteger("otel.logrecord.attribute.count.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes");
    }

    /**
     * Exporter Selection.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#exporter-selection">Exporter Selection</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter exporterSelection(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter
                // OTEL_TRACES_EXPORTER
                .mapEnum("otel.traces.exporter", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type", EXPORTER_TYPES)
                // OTEL_METRICS_EXPORTER
                .mapEnum("otel.metrics.exporter", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type", EXPORTER_TYPES)
                // OTEL_LOGS_EXPORTER
                .mapEnum("otel.logs.exporter", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", EXPORTER_TYPES);
    }

    /**
     * Metrics SDK Configuration.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#metrics-sdk-configuration">Metrics SDK Configuration</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter metrics(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter
                // OTEL_METRICS_EXEMPLAR_FILTER
                .mapEnum("otel.metrics.exemplar.filter", OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".exemplars.filter", EXEMPLAR_FILTERS)
                // OTEL_METRIC_EXPORT_INTERVAL
                .mapDuration("otel.metric.export.interval", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".interval")
                // OTEL_METRIC_EXPORT_TIMEOUT
                .mapTimeout("otel.metric.export.timeout", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout");
    }

    /**
     * OTLP Exporter.
     * <p>
     * All environment variables are supported except:
     * <ul>
     *     <li>{@code OTEL_EXPORTER_OTLP_INSECURE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_TRACES_INSECURE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_METRICS_INSECURE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_LOGS_INSECURE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_CERTIFICATE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_TRACES_CERTIFICATE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_METRICS_CERTIFICATE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_LOGS_CERTIFICATE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_CLIENT_KEY}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_TRACES_CLIENT_KEY}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_METRICS_CLIENT_KEY}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_LOGS_CLIENT_KEY}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_TRACES_CLIENT_CERTIFICATE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_METRICS_CLIENT_CERTIFICATE}</li>
     *     <li>{@code OTEL_EXPORTER_OTLP_LOGS_CLIENT_CERTIFICATE}</li>
     * </ul>
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#otlp-exporter">OTLP Exporter</a>
     */
    static OpenTelemetryEnvironmentPropertyAdapter otlpExporter(OpenTelemetryEnvironmentPropertyAdapter adapter) {
        Assert.notNull(adapter, "adapter cannot be null");
        return adapter

                // OTLP Exporter

                // OTEL_EXPORTER_OTLP_PROTOCOL
                .mapEnum("otel.exporter.otlp.protocol", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.protocol", PROTOCOLS)
                // OTEL_EXPORTER_OTLP_ENDPOINT
                .mapString("otel.exporter.otlp.endpoint", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_HEADERS
                .mapMap("otel.exporter.otlp.headers", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_COMPRESSION
                .mapEnum("otel.exporter.otlp.compression", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.compression", COMPRESSIONS)
                // OTEL_EXPORTER_OTLP_TIMEOUT
                .mapTimeout("otel.exporter.otlp.timeout", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

                // OTLP Exporter (Logs)

                // OTEL_EXPORTER_OTLP_LOGS_PROTOCOL
                .mapEnum("otel.exporter.otlp.logs.protocol", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", PROTOCOLS)
                // OTEL_EXPORTER_OTLP_LOGS_ENDPOINT
                .mapString("otel.exporter.otlp.logs.endpoint", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_LOGS_HEADERS
                .mapMap("otel.exporter.otlp.logs.headers", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_LOGS_COMPRESSION
                .mapEnum("otel.exporter.otlp.logs.compression", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.compression", COMPRESSIONS)
                // OTEL_EXPORTER_OTLP_LOGS_TIMEOUT
                .mapTimeout("otel.exporter.otlp.logs.timeout", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

                // OTLP Exporter (Metrics)

                // OTEL_EXPORTER_OTLP_METRICS_PROTOCOL
                .mapEnum("otel.exporter.otlp.metrics.protocol", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.protocol", PROTOCOLS)
                // OTEL_EXPORTER_OTLP_METRICS_ENDPOINT
                .mapString("otel.exporter.otlp.metrics.endpoint", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_METRICS_HEADERS
                .mapMap("otel.exporter.otlp.metrics.headers", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_METRICS_COMPRESSION
                .mapEnum("otel.exporter.otlp.metrics.compression", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.compression", COMPRESSIONS)
                // OTEL_EXPORTER_OTLP_METRICS_TIMEOUT
                .mapTimeout("otel.exporter.otlp.metrics.timeout", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout")
                // OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE
                .mapEnum("otel.exporter.otlp.metrics.temporality.preference", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".aggregation-temporality", AGGREGATION_TEMPORALITIES)
                // OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION
                .mapEnum("otel.exporter.otlp.metrics.default.histogram.aggregation", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".histogram-aggregation", HISTOGRAM_AGGREGATIONS)

                // OTLP Exporter (Traces)

                // OTEL_EXPORTER_OTLP_TRACES_PROTOCOL
                .mapEnum("otel.exporter.otlp.traces.protocol", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", PROTOCOLS)
                // OTEL_EXPORTER_OTLP_TRACES_ENDPOINT
                .mapString("otel.exporter.otlp.traces.endpoint", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_TRACES_HEADERS
                .mapMap("otel.exporter.otlp.traces.headers", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_TRACES_COMPRESSION
                .mapEnum("otel.exporter.otlp.traces.compression", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.compression", COMPRESSIONS)
                // OTEL_EXPORTER_OTLP_TRACES_TIMEOUT
                .mapTimeout("otel.exporter.otlp.traces.timeout", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.timeout");
    }

}
