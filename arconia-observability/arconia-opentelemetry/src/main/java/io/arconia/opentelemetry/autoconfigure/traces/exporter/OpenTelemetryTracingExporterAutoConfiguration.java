package io.arconia.opentelemetry.autoconfigure.traces.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.arconia.opentelemetry.autoconfigure.traces.ConditionalOnOpenTelemetryTracing;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.console.ConsoleTracingExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingExporterConfiguration;

/**
 * Auto-configuration for exporting OpenTelemetry traces.
 */
@AutoConfiguration(before = org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingAutoConfiguration.class)
@ConditionalOnOpenTelemetryTracing
@Import({ ConsoleTracingExporterConfiguration.class, OtlpTracingExporterConfiguration.class })
@EnableConfigurationProperties(OpenTelemetryTracingExporterProperties.class)
public final class OpenTelemetryTracingExporterAutoConfiguration {}
