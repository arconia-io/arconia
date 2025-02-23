package io.arconia.opentelemetry.autoconfigure.sdk.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import io.arconia.core.config.adapter.PropertyAdapter;
import io.arconia.opentelemetry.autoconfigure.sdk.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.tracing.OpenTelemetryTracingProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.tracing.exporter.OpenTelemetryTracingExporterProperties;

/**
 * Provides adapters from OpenTelemetry SDK properties to Arconia properties.
 */
class OpenTelemetrySdkPropertyAdapters {

    /**
     * General properties for configuring the OpenTelemetry SDK.
     * <p>
     * All properties are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/languages/java/configuration/#properties-general">...</a>
     */
    static PropertyAdapter general(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            // SDK
            .mapProperty("otel.sdk.disabled", OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", value -> !Boolean.parseBoolean(value.toLowerCase()))

            // Attribute Limits
            .mapInteger("otel.attribute.value.length.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".log-limits.max-attribute-value-length")
            .mapInteger("otel.attribute.value.length.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-attribute-value-length")
            .mapInteger("otel.attribute.count.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".log-limits.max-number-of-attributes")
            .mapInteger("otel.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-attributes")

            // Context Propagation
            .mapEnum("otel.propagators", "management.tracing.propagation.produce", OpenTelemetrySdkPropertyConverters::propagationType)

            .build();
    }

    /**
     * Properties for configuring the OpenTelemetry SDK resource.
     * <p>
     * All properties supported, except:
     * <ul>
     *      <li>{@code otel.java.enabled.resource.providers}</li>
     *      <li>{@code otel.java.disabled.resource.providers}</li>
     * </ul>
     *
     * @link <a href="https://opentelemetry.io/docs/languages/java/configuration/#properties-resource">...</a>
     */
    static PropertyAdapter resource(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            // Resource Attributes
            .mapString("otel.service.name", "spring.application.name")
            .mapMap("otel.resource.attributes", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes",
                    // See: https://opentelemetry.io/docs/specs/otel/resource/sdk/#specifying-resource-information-via-an-environment-variable
                    map -> map.entrySet().stream()
                            .map(e -> Map.entry(e.getKey(), URLDecoder.decode(e.getValue(), StandardCharsets.UTF_8)))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
            .mapList("otel.resource.disabled.keys", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".filter.disabled-keys")
            .build();
    }

    /**
     * Properties for configuring the OpenTelemetry SDK logs.
     * <p>
     * All properties supported.
     *
     * @link <a href="https://opentelemetry.io/docs/languages/java/configuration/#properties-logs">...</a>
     */
    static PropertyAdapter logs(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            // Batch Log Record Processor
            .mapDuration("otel.blrp.schedule.delay", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.schedule-delay")
            .mapInteger("otel.blrp.max.queue.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-queue-size")
            .mapInteger("otel.blrp.max.export.batch.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size")
            .mapDuration("otel.blrp.export.timeout", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.exporter-timeout")
            .build();
    }

    /**
     * Properties for configuring the OpenTelemetry SDK metrics.
     * <p>
     * All properties supported, except:
     * <ul>
     *      <li>{@code otel.metrics.exemplar.filter} (still experimental)</li>
     *      <li>{@code otel.experimental.metrics.cardinality.limit} (still experimental)</li>
     * </ul>
     *
     * @link <a href="https://opentelemetry.io/docs/languages/java/configuration/#properties-metrics">...</a>
     */
    static PropertyAdapter metrics(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            .mapDuration("otel.metric.export.interval", OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".interval")
            .build();
    }

    /**
     * Properties for configuring the OpenTelemetry SDK traces.
     * <p>
     * All properties supported.
     *
     * @link <a href="https://opentelemetry.io/docs/languages/java/configuration/#properties-traces">...</a>
     */
    static PropertyAdapter traces(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            // Batch Span Processor
            .mapDuration("otel.bsp.schedule.delay", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.schedule-delay")
            .mapInteger("otel.bsp.max.queue.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-queue-size")
            .mapInteger("otel.bsp.max.export.batch.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size")
            .mapDuration("otel.bsp.export.timeout", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.exporter-timeout")

            // Sampler
            .mapEnum("otel.tracer.sampler", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy", OpenTelemetrySdkPropertyConverters::samplingStrategy)
            .mapDouble("otel.tracer.sampler.arg", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.probability")

            // Span Limits
            .mapInteger("otel.span.attribute.value.length.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-attribute-value-length")
            .mapInteger("otel.span.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-attributes")
            .mapInteger("otel.span.event.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-events")
            .mapInteger("otel.span.link.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".span-limits.max-number-of-links")

            .build();
    }

    /**
     * Properties for configuring the OpenTelemetry exporters (general).
     * <p>
     * All properties supported, except:
     * <ul>
     *      <li>{@code otel.exporter.otlp.{signal}.certificate} (coming soon)</li>
     *      <li>{@code otel.exporter.otlp.{signal}.client.key} (coming soon)</li>
     *      <li>{@code otel.exporter.otlp.{signal}.client.certificate} (coming soon)</li>
     *      <li>{@code otel.java.exporter.otlp.retry.enabled}</li>
     * </ul>
     *
     * @link <a href="https://opentelemetry.io/docs/languages/java/configuration/#properties-exporters">...</a>
     */
    static PropertyAdapter exporters(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            // General
            .mapEnum("otel.logs.exporter", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", OpenTelemetrySdkPropertyConverters::exporterType)
            .mapEnum("otel.metrics.exporter", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type", OpenTelemetrySdkPropertyConverters::exporterType)
            .mapEnum("otel.traces.exporter", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type", OpenTelemetrySdkPropertyConverters::exporterType)
            .mapString("otel.java.exporter.memory_mode", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".memoryMode")

            // OTLP Exporter
            .mapEnum("otel.exporter.otlp.protocol", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetrySdkPropertyConverters::protocol)
            .mapString("otel.exporter.otlp.endpoint", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
            .mapMap("otel.exporter.otlp.headers", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.headers")
            .mapEnum("otel.exporter.otlp.compression", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetrySdkPropertyConverters::compression)
            .mapDuration("otel.exporter.otlp.timeout", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

            // OTLP Exporter (Logs)
            .mapEnum("otel.exporter.otlp.logs.protocol", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetrySdkPropertyConverters::protocol)
            .mapString("otel.exporter.otlp.logs.endpoint", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
            .mapMap("otel.exporter.otlp.logs.headers", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
            .mapEnum("otel.exporter.otlp.logs.compression", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetrySdkPropertyConverters::compression)
            .mapDuration("otel.exporter.otlp.logs.timeout", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

            // OTLP Exporter (Metrics)
            .mapEnum("otel.exporter.otlp.metrics.protocol", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetrySdkPropertyConverters::protocol)
            .mapString("otel.exporter.otlp.metrics.endpoint", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
            .mapMap("otel.exporter.otlp.metrics.headers", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.headers")
            .mapEnum("otel.exporter.otlp.metrics.compression", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetrySdkPropertyConverters::compression)
            .mapDuration("otel.exporter.otlp.metrics.timeout", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

            // Metrics Aggregation
            .mapEnum("otel.exporter.otlp.metrics.default.histogram.aggregation", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".histogram-aggregation", OpenTelemetrySdkPropertyConverters::histogramAggregation)
            .mapEnum("otel.exporter.otlp.metrics.temporality.preference", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".aggregation-temporality", OpenTelemetrySdkPropertyConverters::aggregationTemporality)

            // OTLP Exporter (Traces)
            .mapEnum("otel.exporter.otlp.traces.protocol", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetrySdkPropertyConverters::protocol)
            .mapString("otel.exporter.otlp.traces.endpoint", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
            .mapMap("otel.exporter.otlp.traces.headers", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
            .mapEnum("otel.exporter.otlp.traces.compression", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetrySdkPropertyConverters::compression)
            .mapDuration("otel.exporter.otlp.traces.timeout", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

            .build();
    }

}
