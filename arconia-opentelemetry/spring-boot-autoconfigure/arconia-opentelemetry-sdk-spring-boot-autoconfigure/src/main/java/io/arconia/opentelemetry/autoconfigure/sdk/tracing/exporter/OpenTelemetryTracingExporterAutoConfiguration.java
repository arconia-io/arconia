package io.arconia.opentelemetry.autoconfigure.sdk.tracing.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.sdk.tracing.ConditionalOnEnabledOpenTelemetryTracing;

/**
 * Auto-configuration for exporting OpenTelemetry traces.
 */
@AutoConfiguration
@ConditionalOnEnabledOpenTelemetryTracing
@EnableConfigurationProperties(OpenTelemetryTracingExporterProperties.class)
public class OpenTelemetryTracingExporterAutoConfiguration {}
