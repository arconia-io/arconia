package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.arconia.opentelemetry.autoconfigure.sdk.traces.ConditionalOnOpenTelemetryTracing;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.console.ConsoleTracingExporterConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter.otlp.OtlpTracingExporterConfiguration;

/**
 * Auto-configuration for exporting OpenTelemetry traces.
 */
@AutoConfiguration
@ConditionalOnOpenTelemetryTracing
@Import({ ConsoleTracingExporterConfiguration.class, OtlpTracingExporterConfiguration.class })
@EnableConfigurationProperties(OpenTelemetryTracingExporterProperties.class)
public class OpenTelemetryTracingExporterAutoConfiguration {}
