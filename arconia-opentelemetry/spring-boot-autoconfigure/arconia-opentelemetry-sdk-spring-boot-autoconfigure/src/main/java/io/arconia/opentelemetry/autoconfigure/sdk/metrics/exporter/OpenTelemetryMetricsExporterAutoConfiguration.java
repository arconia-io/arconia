package io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.arconia.opentelemetry.autoconfigure.sdk.metrics.ConditionalOnOpenTelemetryMetrics;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.console.ConsoleMetricsExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

/**
 * Auto-configuration for exporting OpenTelemetry metrics.
 */
@AutoConfiguration
@ConditionalOnOpenTelemetryMetrics
@Import({ ConsoleMetricsExporterConfiguration.class, OtlpMetricsExporterConfiguration.class })
@EnableConfigurationProperties(OpenTelemetryMetricsExporterProperties.class)
public class OpenTelemetryMetricsExporterAutoConfiguration {}
