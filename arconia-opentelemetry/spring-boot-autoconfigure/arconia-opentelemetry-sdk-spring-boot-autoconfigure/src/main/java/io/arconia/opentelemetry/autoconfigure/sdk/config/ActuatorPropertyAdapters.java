package io.arconia.opentelemetry.autoconfigure.sdk.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

import io.arconia.core.config.adapter.PropertyAdapter;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.OpenTelemetryTracingProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.OpenTelemetryTracingExporterProperties;

/**
 * Provides adapters from Spring Boot Actuator properties to Arconia properties.
 */
class ActuatorPropertyAdapters {

    /**
     * Properties for configuring the OpenTelemetry SDK resource.
     * <p>
     * All properties supported.
     */
    static PropertyAdapter resource(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            .mapMap("management.opentelemetry.resource-attributes", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".attributes")
            .build();
    }

    /**
     * Properties for configuring the OpenTelemetry SDK logs and exporters.
     * <p>
     * All properties supported, except:
     * <ul>
     *      <li>{@code management.otlp.logging.connect-timeout}</li>
     * </ul>
     */
    static PropertyAdapter logs(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
            .mapBoolean("management.otlp.logging.export.enabled", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".enabled")
            .mapEnum("management.otlp.logging.transport", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", ActuatorPropertyConverters::protocol)
            .mapString("management.otlp.logging.endpoint", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
            .mapMap("management.otlp.logging.headers", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
            .mapEnum("management.otlp.logging.compression", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.compression", ActuatorPropertyConverters::compression)
            .mapDuration("management.otlp.logging.timeout", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.timeout")
            .build();
    }

    /**
     * Properties for configuring the OpenTelemetry SDK metrics and exporters.
     * <p>
     * All properties supported, except:
     * <ul>
     *      <li>{@code management.otlp.metrics.export.connect-timeout}</li>
     * </ul>
     */
    static PropertyAdapter metrics(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                .mapBoolean("management.otlp.metrics.export.enabled", OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".enabled")

                .mapEnum("management.otlp.metrics.export.aggregation-temporality", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".aggregation-temporality", ActuatorPropertyConverters::aggregationTemporality)
                .mapMap("management.otlp.metrics.export.headers", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                .mapEnum("management.otlp.metrics.export.histogram-flavor", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".histogram-aggregation", ActuatorPropertyConverters::histogramAggregation)
                .mapDuration("management.otlp.metrics.export.read-timeout", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout")
                .mapDuration("management.otlp.metrics.export.step", OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".interval")
                .mapString("management.otlp.metrics.export.url", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")

                .build();
    }

    /**
     * Properties for configuring the OpenTelemetry SDK traces and exporters.
     * <p>
     * All properties supported, except:
     * <ul>
     *      <li>{@code management.otlp.tracing.connect-timeout}</li>
     * </ul>
     */
    static PropertyAdapter traces(ConfigurableEnvironment environment) {
        Assert.notNull(environment, "environment cannot be null");
        return PropertyAdapter.builder(environment)
                .mapBoolean("management.otlp.tracing.export.enabled", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".enabled")
                .mapEnum("management.otlp.tracing.transport", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", ActuatorPropertyConverters::protocol)
                .mapString("management.otlp.tracing.endpoint", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint")
                .mapMap("management.otlp.tracing.headers", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.headers")
                .mapEnum("management.otlp.tracing.compression", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.compression", ActuatorPropertyConverters::compression)
                .mapDuration("management.otlp.tracing.timeout", OpenTelemetryTracingExporterProperties.CONFIG_PREFIX + ".otlp.timeout")
                .build();
    }

}
