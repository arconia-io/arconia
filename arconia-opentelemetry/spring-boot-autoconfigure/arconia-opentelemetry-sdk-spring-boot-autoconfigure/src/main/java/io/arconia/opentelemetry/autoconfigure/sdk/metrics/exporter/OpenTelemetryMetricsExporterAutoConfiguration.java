package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.sdk.metrics.ConditionalOnEnabledOpenTelemetryMetrics;

/**
 * Auto-configuration for exporting OpenTelemetry metrics.
 */
@AutoConfiguration
@ConditionalOnEnabledOpenTelemetryMetrics
@EnableConfigurationProperties(OpenTelemetryMetricsExporterProperties.class)
public class OpenTelemetryMetricsExporterAutoConfiguration {}
