package io.arconia.opentelemetry.autoconfigure.metrics.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.arconia.opentelemetry.autoconfigure.metrics.ConditionalOnOpenTelemetryMetrics;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.console.ConsoleMetricsExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsExporterConfiguration;

/**
 * Auto-configuration for exporting OpenTelemetry metrics.
 */
@AutoConfiguration(before = org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration.class)
@ConditionalOnOpenTelemetryMetrics
@Import({ ConsoleMetricsExporterConfiguration.class, OtlpMetricsExporterConfiguration.class })
@EnableConfigurationProperties(OpenTelemetryMetricsExporterProperties.class)
public class OpenTelemetryMetricsExporterAutoConfiguration {}
