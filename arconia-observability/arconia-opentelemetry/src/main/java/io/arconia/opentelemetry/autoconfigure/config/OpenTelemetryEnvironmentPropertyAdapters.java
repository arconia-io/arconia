package io.arconia.opentelemetry.autoconfigure.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import io.arconia.core.config.adapter.PropertyAdapter;
import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.traces.OpenTelemetryTracingProperties;
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
    static PropertyAdapter general(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                // OTEL_SDK_DISABLED
                .mapProperty("otel.sdk.disabled", OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", value -> !Boolean.parseBoolean(value.toLowerCase()))
                // OTEL_RESOURCE_ATTRIBUTES
                .mapMap("otel.resource.attributes", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes")
                // OTEL_SERVICE_NAME
                .mapString("otel.service.name", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".service-name")
                // OTEL_PROPAGATORS
                .mapEnum("otel.propagators", "management.tracing.propagation.produce", OpenTelemetryEnvironmentPropertyConverters::propagationType)
                // OTEL_TRACES_SAMPLER
                .mapEnum("otel.tracer.sampler", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy", OpenTelemetryEnvironmentPropertyConverters::samplingStrategy)
                // OTEL_TRACES_SAMPLER_ARG
                .mapDouble("otel.tracer.sampler.arg", "management.tracing.sampling.probability")
                .build();
    }

    /**
     * Batch Span Processor.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#batch-span-processor">Batch Span Processor</a>
     */
    static PropertyAdapter batchSpanProcessor(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                // OTEL_BSP_SCHEDULE_DELAY
                .mapDuration("otel.bsp.schedule.delay", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.schedule-delay")
                // OTEL_BSP_EXPORT_TIMEOUT
                .mapDuration("otel.bsp.export.timeout", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.exporter-timeout")
                // OTEL_BSP_MAX_QUEUE_SIZE
                .mapInteger("otel.bsp.max.queue.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-queue-size")
                // OTEL_BSP_MAX_EXPORT_BATCH_SIZE
                .mapInteger("otel.bsp.max.export.batch.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size")
                .build();
    }

    /**
     * Batch Log Record Processor.
     * <p>
     * All properties supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#batch-logrecord-processor">Batch Log Record Processor</a>
     */
    static PropertyAdapter logRecordProcessor(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                // OTEL_BLRP_SCHEDULE_DELAY
                .mapDuration("otel.blrp.schedule.delay", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.schedule-delay")
                // OTEL_BLRP_EXPORT_TIMEOUT
                .mapDuration("otel.blrp.export.timeout", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.exporter-timeout")
                // OTEL_BLRP_MAX_QUEUE_SIZE
                .mapInteger("otel.blrp.max.queue.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-queue-size")
                // OTEL_BLRP_MAX_EXPORT_BATCH_SIZE
                .mapInteger("otel.blrp.max.export.batch.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.max-export-batch-size")
                .build();
    }

    /**
     * Attribute Limits.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#attribute-limits">Attribute Limits</a>
     */
    static PropertyAdapter attributeLimits(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                // OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT
                .mapInteger("otel.attribute.value.length.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length")
                .mapInteger("otel.attribute.value.length.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length")
                // OTEL_ATTRIBUTE_COUNT_LIMIT
                .mapInteger("otel.attribute.count.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes")
                .mapInteger("otel.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes")
                .build();
    }

    /**
     * Span Limits.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#span-limits">Span Limits</a>
     */
    static PropertyAdapter spanLimits(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
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
                .mapInteger("otel.link.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes-per-link")
                .build();
    }

    /**
     * Log Record Limits.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#logrecord-limits">Log Record Limits</a>
     */
    static PropertyAdapter logRecordLimits(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                // OTEL_LOGRECORD_ATTRIBUTE_VALUE_LENGTH_LIMIT
                .mapInteger("otel.logrecord.attribute.value.length.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-attribute-value-length")
                // OTEL_LOGRECORD_ATTRIBUTE_COUNT_LIMIT
                .mapInteger("otel.logrecord.attribute.count.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".limits.max-number-of-attributes")
                .build();
    }

    /**
     * Exporter Selection.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#exporter-selection">Exporter Selection</a>
     */
    static PropertyAdapter exporterSelection(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                // OTEL_TRACES_EXPORTER
                .mapEnum("otel.traces.exporter", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".type", OpenTelemetryEnvironmentPropertyConverters::exporterType)
                // OTEL_METRICS_EXPORTER
                .mapEnum("otel.metrics.exporter", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type", OpenTelemetryEnvironmentPropertyConverters::exporterType)
                // OTEL_LOGS_EXPORTER
                .mapEnum("otel.logs.exporter", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", OpenTelemetryEnvironmentPropertyConverters::exporterType)
                .build();
    }

    /**
     * Metrics SDK Configuration.
     * <p>
     * All environment variables are supported.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/#metrics-sdk-configuration">Metrics SDK Configuration</a>
     */
    static PropertyAdapter metrics(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                // OTEL_METRICS_EXEMPLAR_FILTER
                .mapEnum("otel.metrics.exemplar.filter", OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".exemplars.filter", OpenTelemetryEnvironmentPropertyConverters::exemplarFilter)
                // OTEL_METRIC_EXPORT_INTERVAL
                .mapDuration("otel.metric.export.interval", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".interval")
                // OTEL_METRIC_EXPORT_TIMEOUT
                .mapDuration("otel.metric.export.timeout", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout")
                .build();
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
    static PropertyAdapter otlpExporter(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)

                // OTLP Exporter

                // OTEL_EXPORTER_OTLP_PROTOCOL
                .mapEnum("otel.exporter.otlp.protocol", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetryEnvironmentPropertyConverters::protocol)
                // OTEL_EXPORTER_OTLP_ENDPOINT
                .mapString("otel.exporter.otlp.endpoint", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_HEADERS
                .mapMap("otel.exporter.otlp.headers", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_COMPRESSION
                .mapEnum("otel.exporter.otlp.compression", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetryEnvironmentPropertyConverters::compression)
                // OTEL_EXPORTER_OTLP_TIMEOUT
                .mapDuration("otel.exporter.otlp.timeout", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

                // OTLP Exporter (Logs)

                // OTEL_EXPORTER_OTLP_LOGS_PROTOCOL
                .mapEnum("otel.exporter.otlp.logs.protocol", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetryEnvironmentPropertyConverters::protocol)
                // OTEL_EXPORTER_OTLP_LOGS_ENDPOINT
                .mapString("otel.exporter.otlp.logs.endpoint", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_LOGS_HEADERS
                .mapMap("otel.exporter.otlp.logs.headers", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_LOGS_COMPRESSION
                .mapEnum("otel.exporter.otlp.logs.compression", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetryEnvironmentPropertyConverters::compression)
                // OTEL_EXPORTER_OTLP_LOGS_TIMEOUT
                .mapDuration("otel.exporter.otlp.logs.timeout", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

                // OTLP Exporter (Metrics)

                // OTEL_EXPORTER_OTLP_METRICS_PROTOCOL
                .mapEnum("otel.exporter.otlp.metrics.protocol", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetryEnvironmentPropertyConverters::protocol)
                // OTEL_EXPORTER_OTLP_METRICS_ENDPOINT
                .mapString("otel.exporter.otlp.metrics.endpoint", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_METRICS_HEADERS
                .mapMap("otel.exporter.otlp.metrics.headers", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_METRICS_COMPRESSION
                .mapEnum("otel.exporter.otlp.metrics.compression", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetryEnvironmentPropertyConverters::compression)
                // OTEL_EXPORTER_OTLP_METRICS_TIMEOUT
                .mapDuration("otel.exporter.otlp.metrics.timeout", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

                // OTLP Exporter (Traces)

                // OTEL_EXPORTER_OTLP_TRACES_PROTOCOL
                .mapEnum("otel.exporter.otlp.traces.protocol", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", OpenTelemetryEnvironmentPropertyConverters::protocol)
                // OTEL_EXPORTER_OTLP_TRACES_ENDPOINT
                .mapString("otel.exporter.otlp.traces.endpoint", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                // OTEL_EXPORTER_OTLP_TRACES_HEADERS
                .mapMap("otel.exporter.otlp.traces.headers", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                // OTEL_EXPORTER_OTLP_TRACES_COMPRESSION
                .mapEnum("otel.exporter.otlp.traces.compression", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.compression", OpenTelemetryEnvironmentPropertyConverters::compression)
                // OTEL_EXPORTER_OTLP_TRACES_TIMEOUT
                .mapDuration("otel.exporter.otlp.traces.timeout", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.timeout")

                .build();
    }

}
