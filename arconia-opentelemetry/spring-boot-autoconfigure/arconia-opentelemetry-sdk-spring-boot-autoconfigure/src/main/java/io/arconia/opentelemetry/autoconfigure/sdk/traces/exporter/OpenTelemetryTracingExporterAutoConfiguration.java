package io.arconia.opentelemetry.autoconfigure.sdk.traces.exporter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.sdk.traces.ConditionalOnOpenTelemetryTracing;

/**
 * Auto-configuration for exporting OpenTelemetry traces.
 */
@AutoConfiguration
@ConditionalOnOpenTelemetryTracing
@EnableConfigurationProperties(OpenTelemetryTracingExporterProperties.class)
public class OpenTelemetryTracingExporterAutoConfiguration {}
